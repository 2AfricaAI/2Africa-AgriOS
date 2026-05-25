package ai.toafrica.agrios.production.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.mapper.CropMapper;
import ai.toafrica.agrios.master.mapper.VarietyMapper;
import ai.toafrica.agrios.production.dto.PlantingPlanForm;
import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.mapper.PlantingPlanMapper;
import ai.toafrica.agrios.production.mapper.PlotMapper;
import ai.toafrica.agrios.production.vo.PlantingPlanVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantingPlanService {

    private final PlantingPlanMapper plantingPlanMapper;
    private final PlotMapper plotMapper;
    private final CropMapper cropMapper;
    private final VarietyMapper varietyMapper;

    private static final Set<String> ALL_STATUS = Set.of(
            "draft", "planned", "in_progress", "harvested", "completed", "cancelled");

    // ============================================================
    // 查询
    // ============================================================
    public PageResult<PlantingPlanVO> page(Long plotId, Long cropId, String status,
                                            String code, PageQuery pq) {
        QueryWrapper<PlantingPlanVO> q = new QueryWrapper<>();
        q.isNull("pp.deleted_at");                                          // 软删过滤
        if (plotId != null) q.eq("pp.plot_id", plotId);
        if (cropId != null) q.eq("pp.crop_id", cropId);
        if (status != null && !status.isBlank()) q.eq("pp.status", status.trim());
        if (code != null && !code.isBlank()) q.like("pp.code", code.trim());
        q.orderByDesc("pp.id");

        Page<PlantingPlanVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(plantingPlanMapper.pageWithJoin(p, q));
    }

    public PlantingPlanVO detail(Long id) {
        QueryWrapper<PlantingPlanVO> q = new QueryWrapper<>();
        q.isNull("pp.deleted_at");
        q.eq("pp.id", id);
        Page<PlantingPlanVO> p = new Page<>(1, 1);
        var page = plantingPlanMapper.pageWithJoin(p, q);
        if (page.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "种植计划不存在");
        }
        return page.getRecords().get(0);
    }

    // ============================================================
    // 创建
    // ============================================================
    public Long create(PlantingPlanForm form) {
        validateForm(form, null);

        PlantingPlan p = new PlantingPlan();
        BeanUtils.copyProperties(form, p);
        p.setStatus("draft");
        plantingPlanMapper.insert(p);
        log.info("[新建种植计划] id={} code={} plot={} crop={} variety={}",
                p.getId(), p.getCode(), p.getPlotId(), p.getCropId(), p.getVarietyId());
        return p.getId();
    }

    // ============================================================
    // 修改
    // ============================================================
    public void update(Long id, PlantingPlanForm form) {
        PlantingPlan p = plantingPlanMapper.selectById(id);
        if (p == null) throw new BusinessException(R.NOT_FOUND, "种植计划不存在");
        validateForm(form, id);

        BeanUtils.copyProperties(form, p);
        plantingPlanMapper.updateById(p);
    }

    // ============================================================
    // 状态切换 (自由,不强制状态机)
    // ============================================================
    public void changeStatus(Long id, String status) {
        if (status == null || !ALL_STATUS.contains(status)) {
            throw new BusinessException("status 必须是: " + ALL_STATUS);
        }
        PlantingPlan p = plantingPlanMapper.selectById(id);
        if (p == null) throw new BusinessException(R.NOT_FOUND, "种植计划不存在");
        p.setStatus(status);
        plantingPlanMapper.updateById(p);
    }

    // ============================================================
    // 删除 (软删)
    // ============================================================
    public void delete(Long id) {
        PlantingPlan p = plantingPlanMapper.selectById(id);
        if (p == null) return;
        plantingPlanMapper.deleteById(id);  // @TableLogic 软删
    }

    // ============================================================
    // 内部校验
    // ============================================================
    private void validateForm(PlantingPlanForm form, Long excludeId) {
        // 1. code 唯一
        LambdaQueryWrapper<PlantingPlan> q = new LambdaQueryWrapper<PlantingPlan>()
                .eq(PlantingPlan::getCode, form.getCode());
        if (excludeId != null) q.ne(PlantingPlan::getId, excludeId);
        if (plantingPlanMapper.exists(q)) {
            throw new BusinessException("计划编码已存在: " + form.getCode());
        }

        // 2. 引用合法性
        if (plotMapper.selectById(form.getPlotId()) == null) {
            throw new BusinessException("地块不存在: id=" + form.getPlotId());
        }
        if (cropMapper.selectById(form.getCropId()) == null) {
            throw new BusinessException("作物不存在: id=" + form.getCropId());
        }
        if (form.getVarietyId() != null) {
            var v = varietyMapper.selectById(form.getVarietyId());
            if (v == null) {
                throw new BusinessException("品种不存在: id=" + form.getVarietyId());
            }
            if (!v.getCropId().equals(form.getCropId())) {
                throw new BusinessException("品种与作物不匹配 (该品种属于另一种作物)");
            }
        }

        // 3. 日期顺序
        if (form.getPlanHarvestDate().isBefore(form.getPlanStartDate())) {
            throw new BusinessException("计划采收日期不能早于计划起始日期");
        }
    }
}
