package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.finance.mapper.StatementMapper;
import ai.toafrica.agrios.finance.vo.CustomerStatementVO;
import ai.toafrica.agrios.finance.vo.StatementOrderLine;
import ai.toafrica.agrios.finance.vo.StatementPaymentLine;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 客户对账单服务 - Sprint 16.4.
 *   closingBalance = openingBalance + periodSales - periodPayments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerStatementService {

    private final StatementMapper statementMapper;
    private final CustomerMapper customerMapper;

    public CustomerStatementVO build(Long customerId, LocalDate from, LocalDate to) {
        Customer c = customerMapper.selectById(customerId);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Customer not found");
        if (from == null || to == null) {
            throw new BusinessException(R.BUSINESS_ERROR, "from and to are required");
        }
        if (to.isBefore(from)) {
            throw new BusinessException(R.BUSINESS_ERROR, "to must be >= from");
        }

        CustomerStatementVO vo = new CustomerStatementVO();

        // ---- 客户信息 ----
        vo.setCustomerId(c.getId());
        vo.setCustomerCode(c.getCode());
        vo.setCustomerName(c.getName());
        vo.setCustomerType(c.getType());
        vo.setContactName(c.getContactName());
        vo.setContactPhone(c.getContactPhone());
        vo.setCreditDays(c.getCreditDays());
        vo.setPaymentTerms(c.getPaymentTerms());

        // ---- 期间 ----
        vo.setFromDate(from);
        vo.setToDate(to);
        vo.setGeneratedAt(LocalDate.now());

        // ---- 期初余额 ----
        BigDecimal opening = Objects.requireNonNullElse(
                statementMapper.openingBalance(customerId, from), BigDecimal.ZERO);
        vo.setOpeningBalance(opening);

        // ---- 明细 ----
        List<StatementOrderLine> orders = statementMapper.periodOrders(customerId, from, to);
        List<StatementPaymentLine> payments = statementMapper.periodPayments(customerId, from, to);
        vo.setOrders(orders);
        vo.setPayments(payments);

        // ---- 期间汇总 ----
        BigDecimal periodSales = orders.stream()
                .map(StatementOrderLine::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periodPayments = payments.stream()
                .map(StatementPaymentLine::getAmountKes)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setPeriodSales(periodSales);
        vo.setPeriodPayments(periodPayments);
        vo.setClosingBalance(opening.add(periodSales).subtract(periodPayments));

        // ---- 账龄 (截至 to) ----
        Map<String, Object> aging = statementMapper.agingAsOf(customerId, to);
        vo.setAging0to7   (toBd(aging, "aging_0_7"));
        vo.setAging8to14  (toBd(aging, "aging_8_14"));
        vo.setAging15to30 (toBd(aging, "aging_15_30"));
        vo.setAging30Plus (toBd(aging, "aging_30_plus"));

        log.info("[Statement] customer={} period={}~{} opening={} sales={} payments={} closing={}",
                c.getCode(), from, to, opening, periodSales, periodPayments, vo.getClosingBalance());
        return vo;
    }

    private static BigDecimal toBd(Map<String, Object> m, String key) {
        if (m == null) return BigDecimal.ZERO;
        Object v = m.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return new BigDecimal(v.toString());
    }
}
