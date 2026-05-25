package ai.toafrica.agrios.production.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.production.dto.PlotDTO;
import ai.toafrica.agrios.production.entity.Plot;
import ai.toafrica.agrios.production.mapper.PlotMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlotService {

    private final PlotMapper plotMapper;

    private static final Set<String> ALLOWED_STATUS = Set.of("active", "inactive", "fallow");

    /** 分页查询 */
    public PageResult<PlotMapper.PlotVO> page(PlotMapper.PlotQueryVO query, PageQuery pq) {
        Page<?> page = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(plotMapper.pageWithOwner(page, query == null ? new PlotMapper.PlotQueryVO() : query));
    }

    /** 详情 */
    public Plot detail(Long id) {
        Plot p = plotMapper.selectById(id);
        if (p == null || p.getDeletedAt() != null) {
            throw BusinessException.notFound("地块", id);
        }
        return p;
    }

    /** 详情 + 近 30 天统计 */
    public PlotMapper.PlotStatVO stats(Long id) {
        return plotMapper.statsLast30Days(id);
    }

    /** 新建 */
    @Transactional
    public Long create(PlotDTO dto) {
        // 编号唯一
        QueryWrapper<Plot> w = new QueryWrapper<>();
        w.eq("code", dto.getCode()).isNull("deleted_at");
        if (plotMapper.selectCount(w) > 0) {
            throw new BusinessException("地块编号 " + dto.getCode() + " 已存在");
        }
        Plot p = new Plot();
        BeanUtils.copyProperties(dto, p);
        if (!ALLOWED_STATUS.contains(p.getStatus())) p.setStatus("active");
        Long uid = SecurityUtil.currentUserId();
        p.setCreatedBy(uid);
        p.setUpdatedBy(uid);
        plotMapper.insert(p);
        log.info("[创建地块] code={} name={} by={}", p.getCode(), p.getName(), uid);
        return p.getId();
    }

    /** 编辑 */
    @Transactional
    public void update(Long id, PlotDTO dto) {
        Plot exist = detail(id);
        if (!exist.getCode().equals(dto.getCode())) {
            throw new BusinessException("地块编号一经创建不可修改");
        }
        BeanUtils.copyProperties(dto, exist, "id", "code", "createdAt", "createdBy", "deletedAt");
        exist.setUpdatedBy(SecurityUtil.currentUserId());
        plotMapper.updateById(exist);
        log.info("[编辑地块] id={} by={}", id, exist.getUpdatedBy());
    }

    /** 软删 */
    @Transactional
    public void delete(Long id) {
        Plot p = detail(id);
        // 业务规则：有进行中计划不允许删除（这里简化为检查 status，完整版应查 planting_plan 表）
        if ("active".equals(p.getStatus())) {
            throw new BusinessException("启用中的地块不能删除，请先停用");
        }
        plotMapper.deleteById(id);  // 触发逻辑删除
        log.info("[删除地块] id={}", id);
    }

    /** 启停 */
    @Transactional
    public void toggleStatus(Long id, String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BusinessException("非法状态值：" + status);
        }
        Plot p = detail(id);
        p.setStatus(status);
        p.setUpdatedBy(SecurityUtil.currentUserId());
        plotMapper.updateById(p);
    }
}
