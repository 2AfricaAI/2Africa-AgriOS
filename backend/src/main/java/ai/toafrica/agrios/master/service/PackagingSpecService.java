package ai.toafrica.agrios.master.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.dto.PackagingSpecForm;
import ai.toafrica.agrios.master.entity.PackagingSpec;
import ai.toafrica.agrios.master.mapper.PackagingSpecMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackagingSpecService {

    private final PackagingSpecMapper packagingSpecMapper;

    public PageResult<PackagingSpec> page(String name, String code, Integer status, PageQuery pq) {
        LambdaQueryWrapper<PackagingSpec> q = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) q.like(PackagingSpec::getName, name.trim());
        if (code != null && !code.isBlank()) q.like(PackagingSpec::getCode, code.trim());
        if (status != null) q.eq(PackagingSpec::getStatus, status);
        q.orderByAsc(PackagingSpec::getUnitNetKg);

        Page<PackagingSpec> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(packagingSpecMapper.selectPage(p, q));
    }

    public PackagingSpec detail(Long id) {
        PackagingSpec s = packagingSpecMapper.selectById(id);
        if (s == null) throw new BusinessException(R.NOT_FOUND, "包装规格不存在");
        return s;
    }

    /** 创建 */
    public Long create(PackagingSpecForm form) {
        if (existsByCode(form.getCode(), null)) {
            throw new BusinessException("包装规格编码已存在: " + form.getCode());
        }
        PackagingSpec s = new PackagingSpec();
        BeanUtils.copyProperties(form, s);
        s.setStatus(1);
        packagingSpecMapper.insert(s);
        return s.getId();
    }

    /** 修改 */
    public void update(Long id, PackagingSpecForm form) {
        PackagingSpec s = packagingSpecMapper.selectById(id);
        if (s == null) throw new BusinessException(R.NOT_FOUND, "包装规格不存在");
        if (existsByCode(form.getCode(), id)) {
            throw new BusinessException("包装规格编码已被占用: " + form.getCode());
        }
        BeanUtils.copyProperties(form, s);
        packagingSpecMapper.updateById(s);
    }

    /** 状态切换 */
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能是 0 或 1");
        }
        PackagingSpec s = packagingSpecMapper.selectById(id);
        if (s == null) throw new BusinessException(R.NOT_FOUND, "包装规格不存在");
        s.setStatus(status);
        packagingSpecMapper.updateById(s);
    }

    private boolean existsByCode(String code, Long excludeId) {
        LambdaQueryWrapper<PackagingSpec> q = new LambdaQueryWrapper<PackagingSpec>().eq(PackagingSpec::getCode, code);
        if (excludeId != null) q.ne(PackagingSpec::getId, excludeId);
        return packagingSpecMapper.exists(q);
    }
}
