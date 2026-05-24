package com.albertsfarm.master.service;

import com.albertsfarm.common.PageQuery;
import com.albertsfarm.common.PageResult;
import com.albertsfarm.common.R;
import com.albertsfarm.common.exception.BusinessException;
import com.albertsfarm.master.dto.WarehouseForm;
import com.albertsfarm.master.entity.LocationWarehouse;
import com.albertsfarm.master.mapper.LocationWarehouseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationWarehouseService {

    private final LocationWarehouseMapper warehouseMapper;

    public PageResult<LocationWarehouse> page(
            String name, String code, String type, Long parentId, Integer status, PageQuery pq) {
        LambdaQueryWrapper<LocationWarehouse> q = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) q.like(LocationWarehouse::getName, name.trim());
        if (code != null && !code.isBlank()) q.like(LocationWarehouse::getCode, code.trim());
        if (type != null && !type.isBlank()) q.eq(LocationWarehouse::getType, type.trim());
        if (parentId != null) q.eq(LocationWarehouse::getParentId, parentId);
        if (status != null) q.eq(LocationWarehouse::getStatus, status);
        q.orderByAsc(LocationWarehouse::getParentId).orderByAsc(LocationWarehouse::getCode);

        Page<LocationWarehouse> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(warehouseMapper.selectPage(p, q));
    }

    public LocationWarehouse detail(Long id) {
        LocationWarehouse w = warehouseMapper.selectById(id);
        if (w == null) throw new BusinessException(R.NOT_FOUND, "仓库/库位不存在");
        return w;
    }

    /** 创建 */
    public Long create(WarehouseForm form) {
        validateParentId(form.getParentId(), null);
        if (existsByCode(form.getCode(), null)) {
            throw new BusinessException("库位编码已存在: " + form.getCode());
        }
        LocationWarehouse w = new LocationWarehouse();
        BeanUtils.copyProperties(form, w);
        if (w.getParentId() == null) w.setParentId(0L);
        w.setStatus(1);
        warehouseMapper.insert(w);
        return w.getId();
    }

    /** 修改 */
    public void update(Long id, WarehouseForm form) {
        LocationWarehouse w = warehouseMapper.selectById(id);
        if (w == null) throw new BusinessException(R.NOT_FOUND, "仓库/库位不存在");
        validateParentId(form.getParentId(), id);
        if (existsByCode(form.getCode(), id)) {
            throw new BusinessException("库位编码已被占用: " + form.getCode());
        }
        BeanUtils.copyProperties(form, w);
        if (w.getParentId() == null) w.setParentId(0L);
        warehouseMapper.updateById(w);
    }

    /** 状态切换 */
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能是 0 或 1");
        }
        LocationWarehouse w = warehouseMapper.selectById(id);
        if (w == null) throw new BusinessException(R.NOT_FOUND, "仓库/库位不存在");
        w.setStatus(status);
        warehouseMapper.updateById(w);
    }

    /** parentId == null / 0 表示顶层,合法;否则必须存在,且不能指向自己 */
    private void validateParentId(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) return;
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessException("父节点不能是自己");
        }
        if (warehouseMapper.selectById(parentId) == null) {
            throw new BusinessException("父节点不存在: id=" + parentId);
        }
    }

    private boolean existsByCode(String code, Long excludeId) {
        LambdaQueryWrapper<LocationWarehouse> q = new LambdaQueryWrapper<LocationWarehouse>()
                .eq(LocationWarehouse::getCode, code);
        if (excludeId != null) q.ne(LocationWarehouse::getId, excludeId);
        return warehouseMapper.exists(q);
    }
}
