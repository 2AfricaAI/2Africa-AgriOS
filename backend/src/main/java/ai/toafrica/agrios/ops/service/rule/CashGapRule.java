package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.finance.service.CashFlowForecastService;
import ai.toafrica.agrios.finance.vo.CashFlowForecastVO;
import ai.toafrica.agrios.finance.vo.CashFlowWeekVO;
import ai.toafrica.agrios.ops.entity.ActionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * R-CASH-01 现金缺口预警 - Sprint 18.2 (升级为真实实现).
 *
 *   逻辑:
 *     调 CashFlowForecastService.forecast(openingBalance=0)
 *     → 得到未来 13 周的累计净现金流曲线
 *     → 看 minBalance (即未来累计净流的最低点)
 *     → 这相当于回答: "为了不断现金链, 当前至少需要保留多少现金储备?"
 *
 *   触发条件: minBalance < 0
 *
 *   severity 按缺口深度分级:
 *     <= -500,000 KES   high     (≈ 5000 USD)
 *     -100,000 ~ -500K  medium
 *     -1 ~ -100K        low
 *
 *   description 包含: 最低点周次 / 金额 / 建议储备
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashGapRule implements ActionRule {

    private final CashFlowForecastService forecastService;

    @Override public String ruleCode()  { return "R-CASH-01"; }
    @Override public String category()  { return "week_risk"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "ceo"; }

    @Override
    public List<ActionItem> evaluate() {
        // 用 0 作 opening, 看未来净流动趋势 (= "需要的最低现金储备")
        CashFlowForecastVO vo;
        try {
            vo = forecastService.forecast(BigDecimal.ZERO);
        } catch (Exception e) {
            log.error("[Rule R-CASH-01] cash flow forecast failed", e);
            return List.of();
        }

        BigDecimal minBal = vo.getMinBalance() != null ? vo.getMinBalance() : BigDecimal.ZERO;
        if (minBal.signum() >= 0) {
            return List.of();
        }

        // 缺口 (正数, 单位 KES)
        BigDecimal gap = minBal.negate();
        String sev;
        if (gap.compareTo(new BigDecimal("500000")) >= 0)      sev = "high";
        else if (gap.compareTo(new BigDecimal("100000")) >= 0) sev = "medium";
        else                                                    sev = "low";

        int wkIdx = vo.getMinBalanceWeek() != null ? vo.getMinBalanceWeek() : 0;
        CashFlowWeekVO worstWeek = (wkIdx >= 0 && wkIdx < vo.getWeeks().size())
                ? vo.getWeeks().get(wkIdx) : null;

        ActionItem a = new ActionItem();
        a.setRuleCode(ruleCode());
        a.setCategory(category());
        a.setSeverity(sev);
        a.setOwnerRole(ownerRole());
        a.setTitle(String.format("Cash gap — minimum reserve needed: %s KES (Week %d)",
                gap.toPlainString(), wkIdx + 1));
        a.setDescription(String.format(
                "Without additional inflows, cumulative net cash will reach %s KES "
                + "by week %d (%s). 13-week total: inflow %s, outflow %s, net %s. "
                + "Ensure cash reserves of at least %s KES, or accelerate AR collection / delay AP.",
                minBal.toPlainString(),
                wkIdx + 1,
                worstWeek != null ? worstWeek.getWeekEnd() : "?",
                vo.getTotalInflow13w().toPlainString(),
                vo.getTotalOutflow13w().toPlainString(),
                vo.getNetFlow13w().toPlainString(),
                gap.toPlainString()));
        a.setRefType("cash_flow");
        a.setRefId(0L); // 单条全局告警, 没有具体业务 id
        a.setRefCode("CASH-13W");
        a.setDueDate(java.time.LocalDate.now()); // 立即
        a.setDataSnapshot(String.format(
                "{\"min_balance\":%s,\"min_week\":%d,\"min_date\":\"%s\","
                + "\"total_inflow\":%s,\"total_outflow\":%s,\"net_flow\":%s,\"opening\":0}",
                minBal.toPlainString(), wkIdx,
                worstWeek != null ? worstWeek.getWeekEnd() : "",
                vo.getTotalInflow13w().toPlainString(),
                vo.getTotalOutflow13w().toPlainString(),
                vo.getNetFlow13w().toPlainString()));

        log.info("[Rule R-CASH-01] cash gap detected: minBal={} @week {} severity={}",
                minBal, wkIdx, sev);

        return List.of(a);
    }
}
