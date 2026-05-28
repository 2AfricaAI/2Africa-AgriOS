package ai.toafrica.agrios.system.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.sales.service.SalesOrderService;
import ai.toafrica.agrios.sales.vo.SalesOrderDetailVO;
import ai.toafrica.agrios.sales.vo.SalesOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Sprint 37: customer self-service portal endpoints.
 *
 * Every endpoint:
 *   1. Requires user_type=CUSTOMER (enforced at the top of each handler).
 *   2. Forces customerId = SecurityUtil.currentLinkedCustomerId() before
 *      delegating to the underlying service.  No path/query parameter can
 *      override this — that's the whole point.
 */
@Tag(name = "05 · Portal", description = "Customer self-service endpoints (logged-in customer only)")
@RestController
@RequestMapping("/v1/portal")
@RequiredArgsConstructor
public class PortalController {

    private final SalesOrderService orderService;

    private Long requireCustomerCaller() {
        if (!SecurityUtil.isCurrentCustomer()) {
            throw new BusinessException("This endpoint is for CUSTOMER self-service only");
        }
        Long cid = SecurityUtil.currentLinkedCustomerId();
        if (cid == null) {
            throw new BusinessException("Customer account is not linked to a customer row");
        }
        return cid;
    }

    @Operation(summary = "My orders (always filtered to my customer_id)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/orders")
    public R<PageResult<SalesOrderVO>> myOrders(
            String status,
            String code,
            LocalDate dateFrom,
            LocalDate dateTo,
            PageQuery pq) {
        Long cid = requireCustomerCaller();
        return R.ok(orderService.page(cid, status, code, dateFrom, dateTo, pq));
    }

    @Operation(summary = "My order detail (forbidden if order does not belong to me)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/orders/{id}")
    public R<SalesOrderDetailVO> myOrderDetail(@PathVariable Long id) {
        Long cid = requireCustomerCaller();
        SalesOrderDetailVO d = orderService.detail(id);
        if (d == null || d.getOrder() == null
                || !cid.equals(d.getOrder().getCustomerId())) {
            throw new BusinessException("Order not found or not yours");
        }
        return R.ok(d);
    }
}
