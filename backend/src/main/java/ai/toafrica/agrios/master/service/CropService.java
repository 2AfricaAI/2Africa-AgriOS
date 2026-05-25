package ai.toafrica.agrios.master.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.dto.CropForm;
import ai.toafrica.agrios.master.entity.Crop;
import ai.toafrica.agrios.master.mapper.CropMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CropService {

    private final CropMapper cropMapper;

    public PageResult<Crop> page(String name, String code, String category, Integer status, PageQuery pq) {
        LambdaQueryWrapper<Crop> q = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) q.like(Crop::getName, name.trim());
        if (code != null && !code.isBlank()) q.like(Crop::getCode, code.trim());
        if (category != null && !category.isBlank()) q.eq(Crop::getCategory, category.trim());
        if (status != null) q.eq(Crop::getStatus, status);
        q.orderByDesc(Crop::getId);

        Page<Crop> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(cropMapper.selectPage(p, q));
    }

    public Crop detail(Long id) {
        Crop c = cropMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "作物不存在");
        return c;
    }

    /** 创建 */
    public Long create(CropForm form) {
        if (existsByCode(form.getCode(), null)) {
            throw new BusinessException("作物编码已存在: " + form.getCode());
        }
        Crop c = new Crop();
        BeanUtils.copyProperties(form, c);
        if (c.getUnit() == null || c.getUnit().isBlank()) c.setUnit("kg");
        c.setStatus(1);
        cropMapper.insert(c);
        return c.getId();
    }

    /** 修改 */
    public void update(Long id, CropForm form) {
        Crop c = cropMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "作物不存在");
        if (existsByCode(form.getCode(), id)) {
            throw new BusinessException("作物编码已被占用: " + form.getCode());
        }
        BeanUtils.copyProperties(form, c);
        if (c.getUnit() == null || c.getUnit().isBlank()) c.setUnit("kg");
        cropMapper.updateById(c);
    }

    /** 状态切换 (1=启用 0=停用) */
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能是 0 或 1");
        }
        Crop c = cropMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "作物不存在");
        c.setStatus(status);
        cropMapper.updateById(c);
    }

    /** 编码是否已被其他记录占用; excludeId 用于更新场景排除自身 */
    private boolean existsByCode(String code, Long excludeId) {
        LambdaQueryWrapper<Crop> q = new LambdaQueryWrapper<Crop>().eq(Crop::getCode, code);
        if (excludeId != null) q.ne(Crop::getId, excludeId);
        return cropMapper.exists(q);
    }
}
