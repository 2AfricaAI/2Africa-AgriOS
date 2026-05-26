package ai.toafrica.agrios.procurement.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.procurement.dto.SupplierForm;
import ai.toafrica.agrios.procurement.entity.Supplier;
import ai.toafrica.agrios.procurement.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Supplier 业务服务 - Sprint 17.2.
 *   镜像 CustomerService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public PageResult<Supplier> page(String keyword, String type, String status, PageQuery pq) {
        LambdaQueryWrapper<Supplier> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            q.and(w -> w.like(Supplier::getName, k)
                       .or().like(Supplier::getCode, k)
                       .or().like(Supplier::getContactName, k)
                       .or().like(Supplier::getContactPhone, k));
        }
        if (type != null && !type.isBlank()) q.eq(Supplier::getType, type.trim());
        if (status != null && !status.isBlank()) q.eq(Supplier::getStatus, status.trim());
        q.orderByDesc(Supplier::getId);

        Page<Supplier> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(supplierMapper.selectPage(p, q));
    }

    public Supplier detail(Long id) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BusinessException(R.NOT_FOUND, "Supplier not found");
        return s;
    }

    /** 创建 - 自动生成 code = SUP-NNNNN */
    public Long create(SupplierForm form) {
        Supplier s = new Supplier();
        BeanUtils.copyProperties(form, s);
        s.setCode(nextCode());
        s.setStatus("active");
        if (s.getSinceDate() == null) s.setSinceDate(LocalDate.now());
        supplierMapper.insert(s);
        log.info("[Supplier created] id={} code={} name={}", s.getId(), s.getCode(), s.getName());
        return s.getId();
    }

    public void update(Long id, SupplierForm form) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BusinessException(R.NOT_FOUND, "Supplier not found");
        BeanUtils.copyProperties(form, s);
        supplierMapper.updateById(s);
    }

    /** active / inactive */
    public void changeStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new BusinessException("status must be active or inactive");
        }
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BusinessException(R.NOT_FOUND, "Supplier not found");
        s.setStatus(status);
        supplierMapper.updateById(s);
    }

    public void delete(Long id) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BusinessException(R.NOT_FOUND, "Supplier not found");
        supplierMapper.deleteById(id);
    }

    private String nextCode() {
        int next = supplierMapper.maxCodeSeq() + 1;
        return String.format("SUP-%05d", next);
    }
}
