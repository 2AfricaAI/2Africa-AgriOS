package ai.toafrica.agrios.master.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.dto.InputItemForm;
import ai.toafrica.agrios.master.entity.InputItem;
import ai.toafrica.agrios.master.mapper.InputItemMapper;
import ai.toafrica.agrios.master.vo.InputItemVO;
import ai.toafrica.agrios.procurement.entity.Supplier;
import ai.toafrica.agrios.procurement.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * InputItem CRUD service (Sprint 21.3)
 *  - 列表分页 (name/code/type/supplier/status 过滤)
 *  - 详情
 *  - 创建/修改/启用停用
 *  - 名称 + 编码同时唯一
 *  - VO 携带 default_supplier_name 用于前端展示
 */
@Service
@RequiredArgsConstructor
public class InputItemService {

    private final InputItemMapper inputItemMapper;
    private final SupplierMapper  supplierMapper;

    public PageResult<InputItemVO> page(String code, String name, String inputType,
                                        Long supplierId, String status, PageQuery pq) {
        LambdaQueryWrapper<InputItem> q = new LambdaQueryWrapper<>();
        if (code != null && !code.isBlank())       q.like(InputItem::getCode, code.trim());
        if (name != null && !name.isBlank())       q.and(w -> w.like(InputItem::getName, name.trim())
                                                                .or().like(InputItem::getNameEn, name.trim()));
        if (inputType != null && !inputType.isBlank()) q.eq(InputItem::getInputType, inputType.trim());
        if (supplierId != null)                    q.eq(InputItem::getDefaultSupplierId, supplierId);
        if (status != null && !status.isBlank())   q.eq(InputItem::getStatus, status.trim());
        q.orderByAsc(InputItem::getCode);

        Page<InputItem> p = new Page<>(pq.getPage(), pq.getSize());
        Page<InputItem> raw = inputItemMapper.selectPage(p, q);

        // Batch lookup supplier names
        Set<Long> supIds = raw.getRecords().stream()
                .map(InputItem::getDefaultSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> supName = new HashMap<>();
        if (!supIds.isEmpty()) {
            supplierMapper.selectBatchIds(supIds)
                    .forEach(s -> supName.put(s.getId(), s.getName()));
        }

        Page<InputItemVO> voPage = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        voPage.setRecords(raw.getRecords().stream().map(e -> toVO(e, supName)).toList());
        return PageResult.of(voPage);
    }

    public InputItemVO detail(Long id) {
        InputItem e = inputItemMapper.selectById(id);
        if (e == null) throw new BusinessException(R.NOT_FOUND, "InputItem not found");
        Map<Long, String> supName = new HashMap<>();
        if (e.getDefaultSupplierId() != null) {
            Supplier s = supplierMapper.selectById(e.getDefaultSupplierId());
            if (s != null) supName.put(s.getId(), s.getName());
        }
        return toVO(e, supName);
    }

    public Long create(InputItemForm form) {
        ensureUniqueCode(form.getCode(), null);
        if (form.getDefaultSupplierId() != null && supplierMapper.selectById(form.getDefaultSupplierId()) == null) {
            throw new BusinessException("Supplier not found: " + form.getDefaultSupplierId());
        }
        InputItem e = new InputItem();
        BeanUtils.copyProperties(form, e);
        if (e.getStatus() == null || e.getStatus().isBlank()) e.setStatus("active");
        if (e.getPhiDays() == null) e.setPhiDays(0);
        inputItemMapper.insert(e);
        return e.getId();
    }

    public void update(Long id, InputItemForm form) {
        InputItem e = inputItemMapper.selectById(id);
        if (e == null) throw new BusinessException(R.NOT_FOUND, "InputItem not found");
        ensureUniqueCode(form.getCode(), id);
        if (form.getDefaultSupplierId() != null && supplierMapper.selectById(form.getDefaultSupplierId()) == null) {
            throw new BusinessException("Supplier not found: " + form.getDefaultSupplierId());
        }
        BeanUtils.copyProperties(form, e);
        inputItemMapper.updateById(e);
    }

    public void toggleStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new BusinessException("status must be active or inactive");
        }
        InputItem e = inputItemMapper.selectById(id);
        if (e == null) throw new BusinessException(R.NOT_FOUND, "InputItem not found");
        e.setStatus(status);
        inputItemMapper.updateById(e);
    }

    public void delete(Long id) {
        InputItem e = inputItemMapper.selectById(id);
        if (e == null) throw new BusinessException(R.NOT_FOUND, "InputItem not found");
        inputItemMapper.deleteById(id);
    }

    private void ensureUniqueCode(String code, Long excludeId) {
        if (code == null || code.isBlank()) return;
        LambdaQueryWrapper<InputItem> q = new LambdaQueryWrapper<>();
        q.eq(InputItem::getCode, code.trim());
        if (excludeId != null) q.ne(InputItem::getId, excludeId);
        Long count = inputItemMapper.selectCount(q);
        if (count != null && count > 0) {
            throw new BusinessException("Code already exists: " + code);
        }
    }

    private InputItemVO toVO(InputItem e, Map<Long, String> supplierNameById) {
        InputItemVO v = new InputItemVO();
        BeanUtils.copyProperties(e, v);
        if (e.getDefaultSupplierId() !=