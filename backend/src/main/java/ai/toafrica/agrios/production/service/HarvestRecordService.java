package ai.toafrica.agrios.production.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.production.dto.HarvestRecordForm;
import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.entity.HarvestRecord;
import ai.toafrica.agrios.production.entity.Plot;
import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.mapper.BatchMapper;
import ai.toafrica.agrios.production.mapper.HarvestRecordMapper;
import ai.toafrica.agrios.production.mapper.PlantingPlanMapper;
import ai.toafrica.agrios.production.mapper.PlotMapper;
import ai.toafrica.agrios.production.vo.HarvestRecordVO;
import ai.toafrica.agrios.production.vo.HarvestRow;
import ai.toafrica.agrios.qc.service.PhiCheckService;
import ai.toafrica.agrios.qc.vo.PhiCheckVO;
import ai.toafrica.agrios.system.service.FileService;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HarvestRecordService {

    private final HarvestRecordMapper harvestMapper;
    private final BatchMapper batchMapper;
    private final PlantingPlanMapper planMapper;
    private final PlotMapper plotMapper;
    private final FileService fileService;
    private final PhiCheckService phiCheckService;        // Sprint 23 / Phase 5
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================================================
    // 列表 / 详情
    // ============================================================
    public PageResult<HarvestRecordVO> page(Long plotId, Long planId,
                                            LocalDate dateFrom, LocalDate dateTo,
                                            PageQuery pq) {
        QueryWrapper<HarvestRow> q = new QueryWrapper<>();
        if (plotId != null) q.eq("h.plot_id", plotId);
        if (planId != null) q.eq("h.plan_id", planId);
        if (dateFrom != null) q.ge("h.harvest_date", dateFrom);
        if (dateTo != null) q.le("h.harvest_date", dateTo);
        q.orderByDesc("h.harvest_date").orderByDesc("h.id");

        Page<HarvestRow> p = new Page<>(pq.getPage(), pq.getSize());
        var pageData = harvestMapper.pageWithJoin(p, q);

        List<HarvestRecordVO> vos = pageData.getRecords().stream()
                .map(this::toVO).toList();

        PageResult<HarvestRecordVO> r = new PageResult<>();
        r.setList(vos);
        r.setTotal(pageData.getTotal());
        r.setPage(pageData.getCurrent());
        r.setSize(pageData.getSize());
        return r;
    }

    // ============================================================
    // 创建采收记录 + 自动产 Batch (事务保护)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long create(HarvestRecordForm form) {
        // 1) 幂等
        if (form.getClientUuid() != null && !form.getClientUuid().isBlank()) {
            Long existing = harvestMapper.findIdByClientUuid(form.getClientUuid());
            if (existing != null) {
                log.info("[采收-幂等命中] uuid={} 返回已有 id={}", form.getClientUuid(), existing);
                return existing;
            }
        }

        // 2) 取计划反查 plot/crop
        PlantingPlan plan = planMapper.selectById(form.getPlanId());
        if (plan == null) throw new BusinessException("Planting plan not found");
        Plot plot = plotMapper.selectById(plan.getPlotId());
        if (plot == null) throw new BusinessException("Plot not found");

        // 2.5) Sprint 23 / Phase 5: PHI 安全期阻断
        PhiCheckVO phi = phiCheckService.checkPlan(form.getPlanId(), form.getHarvestDate());
        if (phi.isBlocked()) {
            var first = phi.getBlockingSprays().get(0);
            throw new BusinessException(String.format(
                "PHI block: cannot harvest until %s. " +
                "Last spray of %s (PHI %d days) on %s — wait %d more days.",
                phi.getEarliestSafeDate(),
                first.getInputItemCode(),
                first.getPhiDays(),
                first.getSprayDate(),
                phi.getDaysRemaining()
            ));
        }

        Long varietyId = form.getVarietyId() != null ? form.getVarietyId() : plan.getVarietyId();

        // 3) 生成 harvest code: HV-yyyyMMdd-NNN
        LocalDate date = form.getHarvestDate();
        int hSeq = harvestMapper.countByDate(date) + 1;
        String harvestCode = String.format("HV-%s-%03d", date.format(YMD), hSeq);

        // 4) 先建 Batch (因为 harvest 表的 batch_id NOT NULL,要先有 batch)
        int bSeq = batchMapper.countByDateAndPlot(date, plot.getId()) + 1;
        String batchCode = String.format("B-%s-%s-%02d", date.format(YMD), plot.getCode(), bSeq);

        Batch batch = new Batch();
        batch.setCode(batchCode);
        batch.setPlotId(plot.getId());
        batch.setPlanId(plan.getId());
        batch.setCropId(plan.getCropId());
        batch.setVarietyId(varietyId);
        batch.setHarvestRecordId(0L);  // 占位 - harvest 插完后 update 回填(harvest 和 batch 互引用 NOT NULL,只能先占位)
        batch.setHarvestDate(date);
        batch.setQtyKg(form.getQtyKg());
        batch.setQtyRemainKg(form.getQtyKg());
        batch.setStatus("pending");
        batchMapper.insert(batch);  // INSERT 完后 batch.id 已被 MyBatis-Plus 回填

        // 5) 建 HarvestRecord
        HarvestRecord h = new HarvestRecord();
        h.setCode(harvestCode);
        h.setClientUuid(form.getClientUuid());
        h.setPlotId(plot.getId());
        h.setPlanId(plan.getId());
        h.setCropId(plan.getCropId());
        h.setVarietyId(varietyId);
        h.setBatchId(batch.getId());
        h.setHarvestDate(date);
        h.setQtyKg(form.getQtyKg());
        h.setOperatorId(SecurityUtil.currentUserId());
        h.setPhotos(form.getPhotos() != null ? form.getPhotos() : Collections.emptyList());
        h.setLocationGps(form.getLocationGps());
        h.setRemark(form.getRemark());
        harvestMapper.insert(h);

        // 6) 反填 batch.harvest_record_id (batch 表的 harvest_record_id 是 NOT NULL,
        //    但创建顺序循环依赖: harvest 需要 batch_id, batch 需要 harvest_record_id;
        //    先插 batch 时 harvest_record_id 给 0 占位会违反 NOT NULL,
        //    最稳的做法: batch 表的 harvest_record_id 允许临时占位为新 batch.id,
        //    然后等 harvest 插完再 UPDATE batch.harvest_record_id = harvest.id)
        batch.setHarvestRecordId(h.getId());
        batchMapper.updateById(batch);

        log.info("[采收+批次] harvest={} batch={} plot={} crop={} qty={}",
                harvestCode, batchCode, plot.getCode(), plan.getCropId(), form.getQtyKg());
        return h.getId();
    }

    // ============================================================
    // 删除 (慎用 - 采收为审计性数据)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HarvestRecord h = harvestMapper.selectById(id);
        if (h == null) return;
        // 软删 batch + 删 harvest
        if (h.getBatchId() != null) {
            batchMapper.deleteById(h.getBatchId());  // 软删
        }
        harvestMapper.deleteById(id);
    }

    // ============================================================
    // 内部
    // ============================================================
    private HarvestRecordVO toVO(HarvestRow row) {
        HarvestRecordVO vo = new HarvestRecordVO();
        BeanUtils.copyProperties(row, vo);
        vo.setPhotos(fileService.getFilesByIds(parsePhotosJson(row.getPhotosJson())));
        return vo;
    }

    private List<Long> parsePhotosJson(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
