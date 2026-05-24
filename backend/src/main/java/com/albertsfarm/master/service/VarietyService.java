package com.albertsfarm.master.service;

import com.albertsfarm.common.PageQuery;
import com.albertsfarm.common.PageResult;
import com.albertsfarm.common.R;
import com.albertsfarm.common.exception.BusinessException;
import com.albertsfarm.master.dto.VarietyForm;
import com.albertsfarm.master.entity.Variety;
import com.albertsfarm.master.mapper.CropMapper;
import com.albertsfarm.master.mapper.VarietyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VarietyService {

    private final VarietyMapper varietyMapper;
    private final CropMapper cropMapper;

    public PageResult<Variety> page(Long cropId, String name, String code, Integer status, PageQuery pq) {
        LambdaQueryWrapper<Variety> q = new LambdaQueryWrapper<>();
        if (cropId != null) q.eq(Variety::getCropId, cropId);
        if (name != null && !name.isBlank()) q.like(Variety::getName, name.trim());
        if (code != null && !code.isBlank()) q.like(Variety::getCode, code.trim());
        if (status != null) q.eq(Variety::getStatus, status);
        q.orderByAsc(Variety::getCropId).orderByDesc(Variety::getId);

        Page<Variety> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(varietyMapper.selectPage(p, q));
    }

    public Variety detail(Long id) {
        Variety v = varietyMapper.selectById(id);
        if (v == null) throw new BusinessException(R.NOT_FOUND, "品种不存在");
        return v;
    }

    /** 创建 */
    public Long create(VarietyForm form) {
        validateCropExists(form.getCropId());
        if (existsByCropAndCode(form.getCropId(), form.getCode(), null)) {
            throw new BusinessException("该作物下品种编码已存在: " + form.getCode());
        }
        Variety v = new Variety();
        BeanUtils.copyProperties(form, v);
        v.setStatus(1);
        varietyMapper.insert(v);
        return v.getId();
    }

    /** 修改 */
    public void update(Long id, VarietyForm form) {
        Variety v = varietyMapper.selectById(id);
        if (v == null) throw new BusinessException(R.NOT_FOUND, "品种不存在");
        validateCropExists(form.getCropId());
        if (existsByCropAndCode(form.getCropId(), form.getCode(), id)) {
            throw new BusinessException("该作物下品种编码已被占用: " + form.getCode());
        }
        BeanUtils.copyProperties(form, v);
        varietyMapper.updateById(v);
    }

    /** 状态切换 */
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能是 0 或 1");
        }
        Variety v = varietyMapper.selectById(id);
        if (v == null) throw new BusinessException(R.NOT_FOUND, "品种不存在");
        v.setStatus(status);
        varietyMapper.updateById(v);
    }

    private void validateCropExists(Long cropId) {
        if (cropMapper.selectById(cropId) == null) {
            throw new BusinessException("作物不存在: id=" + cropId);
        }
    }

    private boolean existsByCropAndCode(Long cropId, String code, Long excludeId) {
        LambdaQueryWrapper<Variety> q = new LambdaQueryWrapper<Variety>()
                .eq(Variety::getCropId, cropId)
                .eq(Variety::getCode, code);
        if (excludeId != null) q.ne(Variety::getId, excludeId);
        return varietyMapper.exists(q);
    }
}
