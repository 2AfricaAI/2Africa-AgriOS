package ai.toafrica.agrios.production.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.production.dto.ActivityForm;
import ai.toafrica.agrios.production.entity.Activity;
import ai.toafrica.agrios.production.entity.ActivityInput;
import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.mapper.ActivityInputMapper;
import ai.toafrica.agrios.production.mapper.ActivityMapper;
import ai.toafrica.agrios.production.mapper.PlantingPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import ai.toafrica.agrios.production.vo.ActivityRow;
import ai.toafrica.agrios.production.vo.ActivityVO;
import ai.toafrica.agrios.system.service.FileService;
import ai.toafrica.agrios.system.vo.FileVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityInputMapper activityInputMapper;  // Sprint 23f
    private final PlantingPlanMapper plantingPlanMapper;
    private final FileService fileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> ALL_AUDIT_STATUS = Set.of("pending", "approved", "rejected");

    // ============================================================
    // 列表 / 详情
    // ============================================================
    public PageResult<ActivityVO> page(Long plotId, Long planId, String activityType,
                                       LocalDate dateFrom, LocalDate dateTo,
                                       String auditStatus, PageQuery pq) {
        QueryWrapper<ActivityRow> q = new QueryWrapper<>();
        if (plotId != null) q.eq("a.plot_id", plotId);
        if (planId != null) q.eq("a.plan_id", planId);
        if (activityType != null && !activityType.isBlank()) q.eq("a.activity_type", activityType.trim());
        if (auditStatus != null && !auditStatus.isBlank()) q.eq("a.audit_status", auditStatus.trim());
        if (dateFrom != null) q.ge("a.occur_date", dateFrom);
        if (dateTo != null) q.le("a.occur_date", dateTo);
        q.orderByDesc("a.occur_date").orderByDesc("a.id");

        Page<ActivityRow> p = new Page<>(pq.getPage(), pq.getSize());
        var pageData = activityMapper.pageWithJoin(p, q);

        // enrich photos
        List<ActivityVO> vos = pageData.getRecords().stream()
                .map(this::toVOWithPhotos)
                .toList();

        PageResult<ActivityVO> r = new PageResult<>();
        r.setList(vos);
        r.setTotal(pageData.getTotal());
        r.setPage(pageData.getCurrent());
        r.setSize(pageData.getSize());
        return r;
    }

    public ActivityVO detail(Long id) {
        QueryWrapper<ActivityRow> q = new QueryWrapper<>();
        q.eq("a.id", id);
        Page<ActivityRow> p = new Page<>(1, 1);
        var pageData = activityMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Activity record not found");
        }
        return toVOWithPhotos(pageData.getRecords().get(0));
    }

    // ============================================================
    // 创建
    // ============================================================
    @Transactional
    public Long create(ActivityForm form) {
        // 1) 幂等校验
        if (form.getClientUuid() != null && !form.getClientUuid().isBlank()) {
            Long existing = activityMapper.findIdByClientUuid(form.getClientUuid());
            if (existing != null) {
                log.info("[农事-幂等命中] clientUuid={} 返回已有 id={}", form.getClientUuid(), existing);
                return existing;
            }
        }

        // 2) 校验 plan 存在并取出 plotId 反填
        PlantingPlan plan = plantingPlanMapper.selectById(form.getPlanId());
        if (plan == null) {
            throw new BusinessException("Planting plan not found: id=" + form.getPlanId());
        }
        // 如果前端传了 plotId,校验是否一致;否则取计划的 plot
        if (form.getPlotId() != null && !form.getPlotId().equals(plan.getPlotId())) {
            throw new BusinessException("plotId does not match the plan's plotId");
        }

        Activity a = new Activity();
        BeanUtils.copyProperties(form, a);
        a.setPlotId(plan.getPlotId());
        a.setOperatorId(SecurityUtil.currentUserId());  // 当前阶段用 sys_user.id
        a.setAuditStatus("pending");
        if (a.getPhotos() == null) a.setPhotos(Collections.emptyList());
        activityMapper.insert(a);

        // Sprint 23f: 写投入品明细 (PHI 检查依赖)
        saveInputs(a.getId(), form.getInputs());

        log.info("[新建农事] id={} plan={} type={} date={} photos={} inputs={}",
                a.getId(), a.getPlanId(), a.getActivityType(), a.getOccurDate(),
                a.getPhotos().size(),
                form.getInputs() != null ? form.getInputs().size() : 0);
        return a.getId();
    }

    /** Sprint 23f: 批量写 activity_input (先清空再插, 简单可靠) */
    private void saveInputs(Long activityId, List<ActivityForm.InputLine> lines) {
        // 先清旧的 (update 场景)
        activityInputMapper.delete(new LambdaQueryWrapper<ActivityInput>().eq(ActivityInput::getActivityId, activityId));
        if (lines == null || lines.isEmpty()) return;
        for (ActivityForm.InputLine line : lines) {
            if (line.getInputItemId() == null || line.getQty() == null) continue;
            ActivityInput ai = new ActivityInput();
            ai.setActivityId(activityId);
            ai.setInputItemId(line.getInputItemId());
            ai.setQty(line.getQty());
            ai.setUnit(line.getUnit() != null ? line.getUnit() : "");
            ai.setCost(line.getCost());
            activityInputMapper.insert(ai);
        }
    }

    // ============================================================
    // 修改
    // ============================================================
    public void update(Long id, ActivityForm form) {
        Activity a = activityMapper.selectById(id);
        if (a == null) throw new BusinessException(R.NOT_FOUND, "Activity record not found");

        PlantingPlan plan = plantingPlanMapper.selectById(form.getPlanId());
        if (plan == null) {
            throw new BusinessException("Planting plan not found: id=" + form.getPlanId());
        }

        BeanUtils.copyProperties(form, a);
        a.setPlotId(plan.getPlotId());
        if (a.getPhotos() == null) a.setPhotos(Collections.emptyList());
        activityMapper.updateById(a);

        // Sprint 23f: 投入品明细 (清空重写)
        saveInputs(a.getId(), form.getInputs());
    }

    // ============================================================
    // 审核 (approve / reject)
    // ============================================================
    public void audit(Long id, String status, String remark) {
        if (!ALL_AUDIT_STATUS.contains(status)) {
            throw new BusinessException("auditStatus must be pending / approved / rejected");
        }
        Activity a = activityMapper.selectById(id);
        if (a == null) throw new BusinessException(R.NOT_FOUND, "Activity record not found");

        a.setAuditStatus(status);
        a.setAuditorId(SecurityUtil.currentUserId());
        a.setAuditedAt(LocalDateTime.now());
        a.setAuditRemark(remark);
        activityMapper.updateById(a);
    }

    // ============================================================
    // 删除 (物理删 - 审计性质数据慎用; 这里允许 admin/manager 操作)
    // ============================================================
    public void delete(Long id) {
        activityMapper.deleteById(id);
    }

    // ============================================================
    // 内部: JOIN 行 + photos enrich → VO
    // ============================================================
    private ActivityVO toVOWithPhotos(ActivityRow row) {
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(row, vo);

        // photosJson → List<Long> → List<FileVO>
        List<Long> ids = parsePhotosJson(row.getPhotosJson());
        List<FileVO> files = fileService.getFilesByIds(ids);
        vo.setPhotos(files);
        return vo;
    }

    private List<Long> parsePhotosJson(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("[photos JSON 解析失败] {} - {}", json, e.getMessage());
            return new ArrayList<>();
        }
    }
}
