package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.finance.mapper.CashFlowMapper;
import ai.toafrica.agrios.finance.vo.CashFlowForecastVO;
import ai.toafrica.agrios.finance.vo.CashFlowItemVO;
import ai.toafrica.agrios.finance.vo.CashFlowWeekVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 13 周滚动现金流预测 - Sprint 18.1.
 *
 *   流入: AR due_date (排除被 promise 覆盖的) + 客户承诺还款
 *   流出: AP due_date
 *
 *   bucket: 以本周一为锚, 每 7 天一个桶, 共 13 个桶 (91 天).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashFlowForecastService {

    private static final int WEEKS = 13;
    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MMM d");

    private final CashFlowMapper mapper;

    public CashFlowForecastVO forecast(BigDecimal openingBalance) {
        if (openingBalance == null) openingBalance = BigDecimal.ZERO;

        LocalDate today = LocalDate.now();
        LocalDate weekStart0 = today.with(DayOfWeek.MONDAY); // 本周一
        LocalDate horizonEnd = weekStart0.plusWeeks(WEEKS).minusDays(1); // 13 周后的周日

        // 拉数据
        List<Map<String, Object>> arRows = mapper.arDueInWindow(weekStart0, horizonEnd, today);
        List<Map<String, Object>> promiseRows = mapper.promisesInWindow(today, horizonEnd);
        List<Map<String, Object>> apRows = mapper.apDueInWindow(weekStart0, horizonEnd);

        // 准备 13 个空周
        List<CashFlowWeekVO> weeks = new ArrayList<>(WEEKS);
        for (int i = 0; i < WEEKS; i++) {
            LocalDate ws = weekStart0.plusWeeks(i);
            LocalDate we = ws.plusDays(6);
            CashFlowWeekVO w = new CashFlowWeekVO();
            w.setWeekIndex(i);
            w.setWeekStart(ws);
            w.setWeekEnd(we);
            w.setLabel(String.format("Week %d (%s - %s)", i + 1, ws.format(MD), we.format(MD)));
            w.setInflow(BigDecimal.ZERO);
            w.setOutflow(BigDecimal.ZERO);
            w.setInflowItems(new ArrayList<>());
            w.setOutflowItems(new ArrayList<>());
            weeks.add(w);
        }

        // 1. AR 流入
        for (Map<String, Object> r : arRows) {
            LocalDate due = toDate(r.get("due_date"));
            int idx = weekIndexOf(due, weekStart0);
            if (idx < 0 || idx >= WEEKS) continue;
            BigDecimal amount = toBd(r.get("outstanding"));
            CashFlowItemVO item = new CashFlowItemVO();
            item.setType("order_due");
            item.setRef(String.valueOf(r.get("order_id")));
            item.setName((String) r.get("customer_name"));
            item.setAmount(amount);
            item.setDate(due);
            item.setOrderCode((String) r.get("order_code"));
            CashFlowWeekVO w = weeks.get(idx);
            w.getInflowItems().add(item);
            w.setInflow(w.getInflow().add(amount));
        }

        // 2. 承诺还款 流入 (覆盖 AR)
        for (Map<String, Object> r : promiseRows) {
            LocalDate date = toDate(r.get("promised_date"));
            int idx = weekIndexOf(date, weekStart0);
            if (idx < 0 || idx >= WEEKS) continue;
            BigDecimal amount = toBd(r.get("amount"));
            CashFlowItemVO item = new CashFlowItemVO();
            item.setType("promise");
            item.setRef(String.valueOf(r.get("log_id")));
            item.setName((String) r.get("customer_name"));
            item.setAmount(amount);
            item.setDate(date);
            item.setOrderCode((String) r.get("order_code"));
            CashFlowWeekVO w = weeks.get(idx);
            w.getInflowItems().add(item);
            w.setInflow(w.getInflow().add(amount));
        }

        // 3. AP 流出
        for (Map<String, Object> r : apRows) {
            LocalDate due = toDate(r.get("due_date"));
            int idx = weekIndexOf(due, weekStart0);
            if (idx < 0 || idx >= WEEKS) continue;
            BigDecimal amount = toBd(r.get("outstanding"));
            CashFlowItemVO item = new CashFlowItemVO();
            item.setType("po_due");
            item.setRef(String.valueOf(r.get("po_id")));
            item.setName((String) r.get("supplier_name"));
            item.setAmount(amount);
            item.setDate(due);
            item.setOrderCode((String) r.get("po_code")); // PO code 也塞 orderCode 字段
            CashFlowWeekVO w = weeks.get(idx);
            w.getOutflowItems().add(item);
            w.setOutflow(w.getOutflow().add(amount));
        }

        // 4. 累计余额 + 风险指标
        BigDecimal running = openingBalance;
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        BigDecimal minBalance = openingBalance;
        int minIdx = -1;
        LocalDate minDate = null;
        boolean gap = false;

        for (CashFlowWeekVO w : weeks) {
            w.setNetFlow(w.getInflow().subtract(w.getOutflow()));
            running = running.add(w.getNetFlow());
            w.setCumulativeBalance(running);
            totalIn  = totalIn.add(w.getInflow());
            totalOut = totalOut.add(w.getOutflow());
            if (running.compareTo(minBalance) < 0) {
                minBalance = running;
                minIdx = w.getWeekIndex();
                minDate = w.getWeekEnd();
            }
            if (running.signum() < 0) gap = true;
        }

        CashFlowForecastVO vo = new CashFlowForecastVO();
        vo.setOpeningBalance(openingBalance);
        vo.setGeneratedAt(today);
        vo.setHorizonEnd(horizonEnd);
        vo.setWeeks(weeks);
        vo.setMinBalance(minBalance);
        vo.setMinBalanceWeek(minIdx);
        vo.setMinBalanceDate(minDate);
        vo.setGapAlert(gap);
        vo.setTotalInflow13w(totalIn);
        vo.setTotalOutflow13w(totalOut);
        vo.setNetFlow13w(totalIn.subtract(totalOut));
        vo.setEndingBalance(running);

        log.info("[CashFlow] opening={} 13w in={} out={} net={} ending={} gap={} minBal={} @week {}",
                openingBalance, totalIn, totalOut, totalIn.subtract(totalOut),
                running, gap, minBalance, minIdx);

        return vo;
    }

    // -----------------------------------------------------------------
    private static int weekIndexOf(LocalDate d, LocalDate weekStart0) {
        if (d == null) return -1;
        long days = ChronoUnit.DAYS.between(weekStart0, d);
        if (days < 0) return 0; // 过去到期日 → 算第一周 (今天会还/付)
        return (int) (days / 7);
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return new BigDecimal(v.toString());
    }

    private static LocalDate toDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate d) return d;
        if (v instanceof java.sql.Date sd) return sd.toLocalDate();
        return LocalDate.parse(v.toString());
    }
}
