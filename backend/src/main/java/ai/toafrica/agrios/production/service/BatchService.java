package ai.toafrica.agrios.production.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.packhouse.mapper.PackingMapper;
import ai.toafrica.agrios.packhouse.vo.PackingRow;
import ai.toafrica.agrios.production.dto.BatchSplitForm;
import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.mapper.BatchMapper;
import ai.toafrica.agrios.production.vo.BatchDetailVO;
import ai.toafrica.agrios.production.vo.BatchVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchMapper batchMapper;
    private final PackingMapper packingMapper;

    private static final Set<String> ALL_STATUS = Set.of("pending", "processing", "packed", "sold_out", "lost");

    public PageResult<BatchVO> page(Long plotId, Long planId, String status,
                                    LocalDate dateFrom, LocalDate dateTo,
                                    String code, PageQuery pq) {
        QueryWrapper<BatchVO> q = new QueryWrapper<>();
        q.isNull("b.deleted_at");
        if (plotId != null) q.eq("b.plot_id", plotId);
        if (planId != null) q.eq("b.plan_id", planId);
        if (status != null && !status.isBlank()) q.eq("b.status", status.trim());
        if (code != null && !code.isBlank()) q.like("b.code", code.trim());
        if (dateFrom != null) q.ge("b.harvest_date", dateFrom);
        if (dateTo != null) q.le("b.harvest_date", dateTo);
        q.orderByDesc("b.harvest_date").orderByDesc("b.id");

        Page<BatchVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(batchMapper.pageWithJoin(p, q));
    }

    public BatchVO detail(Long id) {
        QueryWrapper<BatchVO> q = new QueryWrapper<>();
        q.isNull("b.deleted_at");
        q.eq("b.id", id);
        Page<BatchVO> p = new Page<>(1, 1);
        var pageData = batchMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Batch not found");
        }
        return pageData.getRecords().get(0);
    }

    public void changeStatus(Long id, String status) {
        if (!ALL_STATUS.contains(status)) {
            throw new BusinessException("status must be " + ALL_STATUS);
        }
        Batch b = batchMapper.selectById(id);
        if (b == null) throw new BusinessException(R.NOT_FOUND, "Batch not found");
        b.setStatus(status);
        batchMapper.updateById(b);
    }

    // ============================================================
    // 详情(含父/子/包装单)- 用于批次详情页溯源
    // ============================================================
    public BatchDetailVO detailFull(Long id) {
        BatchVO self = detail(id);
        BatchDetailVO vo = new BatchDetailVO();
        vo.setBatch(self);

        // 父批次
        if (self.getParentBatchId() != null) {
            try {
                vo.setParent(detail(self.getParentBatchId()));
            } catch (BusinessException ignore) {
                vo.setParent(null);
            }
        }

        // 子批次列表 (用 join 视图,带 plotName/cropName)
        QueryWrapper<BatchVO> qChildren = new QueryWrapper<>();
        qChildren.isNull("b.deleted_at");
        qChildren.eq("b.parent_batch_id", id);
        qChildren.orderByAsc("b.id");
        Page<BatchVO> pChildren = new Page<>(1, 200);
        List<BatchVO> children = batchMapper.pageWithJoin(pChildren, qChildren).getRecords();
        vo.setChildren(children == null ? new ArrayList<>() : children);

        // 关联包装单
        QueryWrapper<PackingRow> qPack = new QueryWrapper<>();
        qPack.eq("pk.batch_id", id);
        qPack.orderByDesc("pk.packed_at").orderByDesc("pk.id");
        Page<PackingRow> pPack = new Page<>(1, 200);
        List<PackingRow> packings = packingMapper.pageWithJoin(pPack, qPack).getRecords();
        vo.setPackings(packings == null ? new ArrayList<>() : packings);

        return vo;
    }

    // ============================================================
    // 拆分批次 - 一个 batch 拆成 N 个小 batch
    //   规则:
    //     1. 父批次必须存在、未删、状态 pending / processing
    //     2. 子批次 qtyKg 之和 ≤ 父批次 qty_remain_kg
    //     3. 子批次继承 plot/plan/crop/variety/harvest_record/harvest_date
    //     4. 子批次 code 形如 {parentCode}-S{seq},seq 接上已有子批次个数
    //     5. 父批次 qty_remain_kg 减去拆出的总量,若归零进入 processing 不变(由包装决定)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public List<Long> split(Long parentId, BatchSplitForm form) {
        Batch parent = batchMapper.selectById(parentId);
        if (parent == null || parent.getDeletedAt() != null) {
            throw new BusinessException(R.NOT_FOUND, "Parent batch not found");
        }
        if (!"pending".equals(parent.getStatus()) && !"processing".equals(parent.getStatus())) {
            throw new BusinessException("Batch current status does not allow splitting: " + parent.getStatus());
        }

        // 累计要拆出的总量
        BigDecimal totalSplit = BigDecimal.ZERO;
        for (BatchSplitForm.Child c : form.getChildren()) {
            if (c.getQtyKg() == null || c.getQtyKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Child batch qtyKg must be > 0");
            }
            totalSplit = totalSplit.add(c.getQtyKg());
        }
        if (totalSplit.compareTo(parent.getQtyRemainKg()) > 0) {
            throw new BusinessException(String.format(
                    "Split total %s kg exceeds parent batch remaining %s kg",
                    totalSplit.toPlainString(), parent.getQtyRemainKg().toPlainString()));
        }

        // 当前已有的子批次个数(用于命名续号)
        QueryWrapper<Batch> qExist = new QueryWrapper<>();
        qExist.isNull("deleted_at");
        qExist.eq("parent_batch_id", parentId);
        int existing = Math.toIntExact(batchMapper.selectCount(qExist));

        List<Long> newIds = new ArrayList<>();
        int seq = existing;
        for (BatchSplitForm.Child c : form.getChildren()) {
            seq++;
            Batch child = new Batch();
            child.setCode(parent.getCode() + "-S" + seq);
            child.setParentBatchId(parent.getId());
            child.setPlotId(parent.getPlotId());
            child.setPlanId(parent.getPlanId());
            child.setCropId(parent.getCropId());
            child.setVarietyId(parent.getVarietyId());
            child.setHarvestRecordId(parent.getHarvestRecordId());
            child.setHarvestDate(parent.getHarvestDate());
            child.setQtyKg(c.getQtyKg());
            child.setQtyRemainKg(c.getQtyKg());
            child.setStatus("pending");
            child.setRemark(c.getRemark());
            batchMapper.insert(child);
            newIds.add(child.getId());
        }

        // 扣父批次余量
        BigDecimal newRemain = parent.getQtyRemainKg().subtract(totalSplit);
        parent.setQtyRemainKg(newRemain);
        // 若拆光,父批次状态进入 processing (表示已物理拆分,等待后续动作)
        if (newRemain.compareTo(BigDecimal.ZERO) == 0 && "pending".equals(parent.getStatus())) {
            parent.setStatus("processing");
        }
        batchMapper.updateById(parent);

        log.info("[批次拆分] 父={} 拆出 {} 个子批次 总量={} kg 父剩余={} kg",
                parent.getCode(), newIds.size(),
                totalSplit.toPlainString(), newRemain.toPlainString());

        return newIds;
    }
}
