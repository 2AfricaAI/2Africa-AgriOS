package ai.toafrica.agrios.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 客户对账单 (Statement of Account) - Sprint 16.4.
 *   所有金额以 KES 本位币展示.
 *   余额公式: closingBalance = openingBalance + periodSales - periodPayments
 */
@Data
@Schema(description = "客户对账单 (Statement of Account)")
public class CustomerStatementVO {
    // ---- 客户信息 ----
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String customerType;
    private String contactName;
    private String contactPhone;
    private Integer creditDays;
    private String paymentTerms;

    // ---- 期间 ----
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate generatedAt;

    // ---- 余额 (KES) ----
    private BigDecimal openingBalance;   // 期初余额 (期前 已开票 - 已收)
    private BigDecimal periodSales;      // 期间销售
    private BigDecimal periodPayments;   // 期间收款
    private BigDecimal closingBalance;   // 期末余额

    // ---- 账龄 (截至 toDate) ----
    private BigDecimal aging0to7;
    private BigDecimal aging8to14;
    private BigDecimal aging15to30;
    private BigDecimal aging30Plus;

    // ---- 明细 ----
    private List<StatementOrderLine> orders;
    private List<StatementPaymentLine> payments;
}
