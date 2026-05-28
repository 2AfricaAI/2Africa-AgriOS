package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.qc.entity.Recall;
import ai.toafrica.agrios.qc.entity.RecallAffectedOrder;
import ai.toafrica.agrios.qc.mapper.RecallAffectedOrderMapper;
import ai.toafrica.agrios.qc.mapper.RecallMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * R-COMP-02 — Recall customer notification (Sprint 27).
 *
 * For every recall in 'initiated' or 'customers_notified' status, generate
 * one action_item per affected_order whose notified_at IS NULL. Sales is
 * expected to contact each customer and mark them as notified via the
 * recall detail page.
 *
 *   tab        = today
 *   severity   = urgent
 *   owner_role = sales
 *   ref        = recall_affected_order.id
 *
 * Items auto-resolve when the corresponding affected_order.notified_at is set
 * (ActionEngineService de-duplicates by uk_rule_ref unique key, and the snapshot
 * comparison + close-out happens on the next refresh.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecallNotificationRule implements ActionRule {

    private final RecallMapper recallMapper;
    private final RecallAffectedOrderMapper affectedMapper;

    @Override public String ruleCode()  { return "R-COMP-02"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "urgent"; }
    @Override public String ownerRole() { return "sales"; }

    @Override
    public List<ActionItem> evaluate() {
        List<Recall> openRecalls = recallMapper.selectList(
                new LambdaQueryWrapper<Recall>()
                        .in(Recall::getStatus, "initiated", "customers_notified"));

        List<ActionItem> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Recall recall : openRecalls) {
            List<RecallAffectedOrder> pending = affectedMapper.selectList(
                    new LambdaQueryWrapper<RecallAffectedOrder>()
                            .eq(RecallAffectedOrder::getRecallId, recall.getId())
                            .isNull(RecallAffectedOrder::getNotifiedAt));
            for (RecallAffectedOrder rao : pending) {
                ActionItem a = new ActionItem();
                a.setRuleCode(ruleCode());
                a.setCategory(category());
                a.setSeverity(severity());
                a.setOwnerRole(ownerRole());
                a.setTitle(String.format("Recall notify — %s (%s, %s units)",
                        rao.getCustomerName(), rao.getOrderCode(),
                        rao.getQty() == null ? "?" : rao.getQty().stripTrailingZeros().toPlainString()));
                a.setDescription(String.format(
                        "Recall %s — contact customer immediately. Order %s, %s units delivered %s.",
                        recall.getCode(), rao.getOrderCode(),
                        rao.getQty() == null ? "?" : rao.getQty().stripTrailingZeros().toPlainString(),
                        rao.getDeliveredAt() == null ? "(unknown)" : rao.getDeliveredAt()));
                a.setRefType("recall_affected_order");
                a.setRefId(rao.getId());
                a.setRefCode(recall.getCode());
                a.setDueDate(today);
                a.setDataSnapshot(String.format(
                        "{\"recall_id\":%d,\"recall_code\":\"%s\",\"order_id\":%d,\"customer_id\":%d}",
                        recall.getId(), recall.getCode(), rao.getOrderId(), rao.getCustomerId()));
                out.add(a);
            }
        }
        if (!out.isEmpty()) {
            log.info("[Rule R-COMP-02] recall notification items = {}", out.size());
        }
        return out;
    }
}
