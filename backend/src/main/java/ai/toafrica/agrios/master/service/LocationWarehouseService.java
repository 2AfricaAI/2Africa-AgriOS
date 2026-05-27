package ai.toafrica.agrios.master.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.dto.WarehouseForm;
import ai.toafrica.agrios.master.entity.LocationWarehouse;
import ai.toafrica.agrios.master.mapper.LocationWarehouseMapper;
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
            String name, String code, String type, String purpose, String level,
            Long parentId, Integer status, PageQuery pq) {
        LambdaQueryWrapper<LocationWarehouse> q = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank())       q.like(LocationWarehouse::getName, name.trim());
        if (code != null && !code.isBlank())       q.like(LocationWarehouse::getCode, code.trim());
        if (type != null && !type.isBlank())       q.eq(LocationWarehouse::getType, type.trim());
        if (purpose != null && !purpose.isBlank()) q.eq(LocationWarehouse::getPurpose, purpose.trim());
        if (level != null && !level.isBlank())     q.eq(LocationWarehouse::getLevel, level.trim());
        if (parentId != null) q.eq(LocationWarehouse::getParentId, parentId);
        if (status != null)   q.eq(LocationWarehouse::getStatus, status);
        q.orderByAsc(LocationWarehouse::getPurpose)
         .orderByAsc(LocationWarehouse::getLevel)
         .orderByAsc(LocationWarehouse::getParentId)
         .orderByAsc(LocationWarehouse::getCode);

        Page<LocationWarehouse> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(warehouseMapper.selectPage(p, q));
    }

    // ====================================================================
    // Sprint 22.0.5 hierarchy helpers - used by input_stock (Sprint 22+)
    // ====================================================================

    /** True if this node has no children (can hold stock if not 'warehouse' level). */
    public boolean isLeaf(Long id) {
        LambdaQueryWrapper<LocationWarehouse> q = new LambdaQueryWrapper<>();
        q.eq(LocationWarehouse::getParentId, id);
        Long n = warehouseMapper.selectCount(q);
        return n == null || n == 0;
    }

    /** True if this node can directly hold input_stock - leaf AND not a warehouse-level container. */
    public boolean isStockable(LocationWarehouse w) {
        if (w == null) return false;
        if ("warehouse".equals(w.getLevel())) return false;  // warehouse must drill down
        return isLeaf(w.getId());
    }

    /** Hard delete - only allowed if (1) node exists, (2) no children. */
    public void delete(Long id) {
        LocationWarehouse w = warehouseMapper.selectById(id);
        if (w == null) throw new BusinessException(R.NOT_FOUND, "Warehouse not found: " + id);
        if (!isLeaf(id)) {
            throw new BusinessException(
                "Cannot delete '" + w.getCode() + "' - it has sub-locations. Delete the children first.");
        }
        // TODO[Sprint 22]: also check input_stock + inventory references before allowing
        warehouseMapper.deleteById(id);
    }

    /** Throws if attempting to stock at a non-stockable node. */
    public void requireStockable(Long warehouseId) {
        LocationWarehouse w = warehouseMapper.selectById(warehouseId);
        if (w == null) throw new BusinessException(R.NOT_FOUND, "Warehouse not found: " + warehouseId);
        if ("warehouse".equals(w.getLevel())) {
            throw new BusinessException(
                "Cannot stock at warehouse-level container '" + w.getCode() + "' - drill down to a zone/shelf/bin");
        }
        if (!isLeaf(w.getId())) {
            throw new BusinessException(
                "Cannot stock at non-leaf node '" + w.getCode() + "' - has sub-locations");
        }
    }

    public LocationWarehouse detail(Long id) {
        LocationWarehouse w = warehouseMapper.selectById(id);
        if (w == null) throw new BusinessException(R.NOT_FOUND, "Warehouse/location not found");
        return w;
    }

    /** 创建 */
    public Long create(WarehouseForm form) {
        validateParentId(form.getParentId(), null);
        if (existsByCode(form.getCode(), null)) {
            throw new BusinessException("Location code already exists: " + form.getCode());
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
        if (w == null) throw new BusinessException(R.NOT_FOUND, "Warehouse/location not found");
        validateParentId(form.getParentId(), id);
        if (existsByCode(form.getCode(), id)) {
            throw new BusinessException("Location code is already in use: " + form.getCode());
        }
        BeanUtils.copyProperties(form, w);
        if (w.getParentId() == null) w.setParentId(0L);
        warehouseMapper.updateById(w);
    }

    /** 状态切换 */
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status must be 0 or 1");
        }
        LocationWarehouse w = warehouseMapper.selectById(id);
        if (w == null) throw new BusinessException(R.NOT_FOUND, "Warehouse/location not found");
        w.setStatus(status);
        warehouseMapper.updateById(w);
    }

    /** parentId == null / 0 表示顶层,合法;否则必须存在,且不能指向自己 */
    private void validateParentId(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) return;
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessException("Parent node cannot be itself");
        }
        if (warehouseMapper.selectById(parentId) == null) {
            throw new BusinessException("Parent node not found: id=" + parentId);
        }
    }

    private boolean existsByCode(String code, Long excludeId) {
        LambdaQueryWrapper<LocationWarehouse> q = new LambdaQueryWrapper<LocationWarehouse>()
                .eq(LocationWarehouse::getCode, code);
        if (excludeId != null) q.ne(LocationWarehouse::getId, excludeId);
        return warehouseMapper.exists(q);
    }
}
