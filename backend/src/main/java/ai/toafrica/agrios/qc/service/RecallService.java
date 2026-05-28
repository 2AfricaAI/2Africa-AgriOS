package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.mapper.BatchMapper;
import ai.toafrica.agrios.qc.dto.RecallForm;
import ai.toafrica.agrios.qc.entity.Complaint;
import ai.toafrica.agrios.qc.entity.Recall;
import ai.toafrica.agrios.qc.entity.RecallAffectedOrder;
import ai.toafrica.agrios.qc.mapper.ComplaintMapper;
import ai.toafrica.agrios.qc.mapper.RecallAffectedOrderMapper;
import ai.toafrica.agrios.qc.mapper.RecallMapper;
import ai.toafrica.agrios.qc.vo.RecallDetailVO;
import ai.toafrica.agrios.qc.vo.RecallVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Batch recall service (Sprint 27).
 *
 * trigger() is the transactional core: it
 *   1. Validates the batch exists.
 *   2. Sums + freezes all inventory rows of that batch (status normal → frozen).
 *   3. Reverse-looks-up downstream orders via fulfillment_item.
 *   4. Snapshots affected orders into recall_affected_order.
 *   5. Updates the source complaint (if any) to status=escalated_to_recall.
 *   6. Creates the recall record with denormalized counts.
 *
 * Notification action_items are produced by the RecallNotificationRule
 * on the next engine run (or via explicit refresh).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecallService {

    private final RecallMapper recallMapper;
    private final RecallAffectedOrderMapper affectedMapper;
    private final ComplaintMapper complaintMapper;
    private final InventoryMapper inventoryMapper;
    private final BatchMapper batchMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ------------------------------------------------------------
    // List
    // ------------------------------------------------------------
    public PageResult<RecallVO> page(Long batchId, String status, PageQuery pq) {
        QueryWrapper<RecallVO> q = new QueryWrapper<>();
        if (batchId != null) q.eq("r.batch_id", batchId);
        if (status != null && !status.isBlank()) q.eq("r.status", status.trim());
        q.orderByDesc("r.triggered_at").orderByDesc("r.id");
        Page<RecallVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(recallMapper.pageWithJoin(p, q));
    }

    public RecallDetailVO detail(Long id) {
        QueryWrapper<RecallVO> q = new QueryWrapper<>();
        q.eq("r.id", id);
        Page<RecallVO> p = new Page<>(1, 1);
        var pageData = recallMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Recall not found");
        }
        RecallDetailVO d = new RecallDetailVO();
        d.setRecall(pageData.getRecords().get(0));
        d.setAffectedOrders(affectedMapper.findByRecallId(id));
        return d;
    }

    // ------------------------------------------------------------
    // Trigger recall — the heavy lift
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public Long trigger(RecallForm form) {
        // 1) Validate batch
        Batch batch = batchMapper.selectById(form.getBatchId());
        if (batch == null) throw new BusinessException("Batch not found: " + form.getBatchId());

        // 2) Sum inventory to freeze, then freeze
        BigDecimal frozenQty = recallMapper.sumFrozenQty(form.getBatchId());
        List<Inventory> rows = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getBatchId, form.getBatchId())
                        .eq(Inventory::getStatus, "normal"));
        for (Inventory inv : rows) {
            inv.setStatus("frozen");
            inv.setLastOpAt(LocalDateTime.now());
            inventoryMapper.updateById(inv);
        }

        // 3) Reverse-lookup downstream orders
        List<Map<String, Object>> affectedOrders = recallMapper.findAffectedOrders(form.getBatchId());

        // 4) Generate recall code + insert
        LocalDate today = LocalDate.now();
        int seq = recallMapper.countByDate(today) + 1;
        Recall recall = new Recall();
        recall.setCode(String.format("RECALL-%s-%04d", today.format(YMD), seq));
        recall.setTriggeredAt(LocalDateTime.now());
        recall.setSourceComplaintId(form.getSourceComplaintId());
        recall.setBatchId(form.getBatchId());
        recall.setScope(form.getScope() == null ? "batch_only" : form.getScope());
        recall.setReason(form.getReason());
        recall.setStatus(affectedOrders.isEmpty() ? "quarantined" : "initiated");
        recall.setAffectedOrderCount(affectedOrders.size());
        Set<Long> distinctCustomers = new HashSet<>();
        for (Map<String, Object> row : affectedOrders) {
            distinctCustomers.add(((Number) row.get("customer_id")).longValue());
        }
        recall.setAffectedCustomerCount(distinctCustomers.size());
        recall.setAffectedQty(frozenQty);
        recall.setInitiatedById(SecurityUtil.currentUserId());
        recallMapper.insert(recall);

        // 5) Snapshot affected orders
        for (Map<String, Object> row : affectedOrders) {
            RecallAffectedOrder rao = new RecallAffectedOrder();
            rao.setRecallId(recall.getId());
            rao.setOrderId(((Number) row.get("order_id")).longValue());
            rao.setOrderCode((String) row.get("order_code"));
            rao.setCustomerId(((Number) row.get("customer_id")).longValue());
            rao.setCustomerName((String) row.get("customer_name"));
            Object qty = row.get("qty");
            rao.setQty(qty == null ? BigDecimal.ZERO : new BigDecimal(qty.toString()));
            rao.setUnit("pack");
            Object delivered = row.get("delivered_at");
            if (delivered instanceof java.sql.Timestamp ts) rao.setDeliveredAt(ts.toLocalDateTime());
            else if (delivered instanceof LocalDateTime ldt) rao.setDeliveredAt(ldt);
            affectedMapper.insert(rao);
        }

        // 6) If linked to a complaint, escalate it
        if (form.getSourceComplaintId() != null) {
            Complaint c = complaintMapper.selectById(form.getSourceComplaintId());
            if (c != null) {
                c.setStatus("escalated_to_recall");
                c.setRecallId(recall.getId());
                complaintMapper.updateById(c);
            }
        }

        log.info("[Recall triggered] code={} batch={} frozenInventoryRows={} affectedOrders={} frozenQty={}",
                recall.getCode(), batch.getCode(), rows.size(),
                affectedOrders.size(), frozenQty.toPlainString());
        return recall.getId();
    }

    // ------------------------------------------------------------
    // Mark a single customer as notified
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void markNotified(Long recallId, Long affectedOrderId) {
        RecallAffectedOrder rao = affectedMapper.selectById(affectedOrderId);
        if (rao == null || !rao.getRecallId().equals(recallId)) {
            throw new BusinessException(R.NOT_FOUND, "Affected order not found");
        }
        rao.setNotifiedAt(LocalDateTime.now());
        rao.setNotifiedById(SecurityUtil.currentUserId());
        affectedMapper.updateById(rao);

        // If all customers in this recall have been notified, advance recall.status
        Long pending = affectedMapper.selectCount(
                new LambdaQueryWrapper<RecallAffectedOrder>()
                        .eq(RecallAffectedOrder::getRecallId, recallId)
                        .isNull(RecallAffectedOrder::getNotifiedAt));
        if (pending == 0) {
            Recall recall = recallMapper.selectById(recallId);
            if (recall != null && "initiated".equals(recall.getStatus())) {
                recall.setStatus("customers_notified");
                recallMapper.updateById(recall);
            }
        }
    }

    // ------------------------------------------------------------
    // Close recall
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, String remark) {
        Recall r = recallMapper.selectById(id);
        if (r == null) throw new BusinessException(R.NOT_FOUND, "Recall not found");
        if ("closed".equals(r.getStatus())) {
            throw new BusinessException("Recall already closed");
        }
        r.setStatus("closed");
        r.setClosedAt(LocalDateTime.now());
        r.setClosedById(SecurityUtil.currentUserId());
        r.setClosedRemark(remark);
        recallMapper.updateById(r);
    }
}
