package ai.toafrica.agrios.finance.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.finance.dto.PaymentForm;
import ai.toafrica.agrios.finance.entity.Payment;
import ai.toafrica.agrios.finance.service.AccountsReceivableService;
import ai.toafrica.agrios.finance.service.CustomerStatementService;
import ai.toafrica.agrios.finance.service.PaymentService;
import ai.toafrica.agrios.finance.service.StatementPdfService;
import ai.toafrica.agrios.finance.vo.CustomerStatementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "51 · Finance Payments + AR", description = "Payments + Accounts Receivable")
@RestController
@RequestMapping("/v1/finance")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AccountsReceivableService arService;
    private final CustomerStatementService statementService;
    private final StatementPdfService statementPdfService;

    @Operation(summary = "List payments")
    @GetMapping("/payments")
    public R<PageResult<Payment>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String method,
            PageQuery pq) {
        return R.ok(paymentService.page(orderId, customerId, method, pq));
    }

    @Operation(summary = "Record a payment")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping("/payments")
    public R<Long> create(@Valid @RequestBody PaymentForm form) {
        return R.ok(paymentService.create(form));
    }

    @Operation(summary = "Reverse a payment (soft delete)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/payments/{id}")
    public R<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Accounts receivable aging by customer (0-7 / 8-14 / 15-30 / 30+ days)")
    @GetMapping("/ar/aging")
    public R<List<Map<String, Object>>> arAging() {
        return R.ok(arService.arAgingByCustomer());
    }

    @Operation(summary = "Customer statement of account (opening + period activity + closing + aging)")
    @GetMapping("/customers/{customerId}/statement")
    public R<CustomerStatementVO> statement(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(statementService.build(customerId, from, to));
    }

    @Operation(summary = "Customer statement of account — downloadable PDF")
    @GetMapping("/customers/{customerId}/statement.pdf")
    public ResponseEntity<byte[]> statementPdf(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = statementPdfService.renderPdf(customerId, from, to);
        String filename = String.format("statement-%d-%s-%s.pdf", customerId, from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.inline().filename(filename).build());
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
    }
}
