package ai.toafrica.agrios.finance.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.finance.service.CashFlowForecastService;
import ai.toafrica.agrios.finance.vo.CashFlowForecastVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "54 · Finance Cash Flow", description = "13-Week Rolling Cash Flow Forecast (Sprint 18)")
@RestController
@RequestMapping("/v1/finance/cash-flow")
@RequiredArgsConstructor
public class CashFlowForecastController {

    private final CashFlowForecastService service;

    @Operation(summary = "13-week rolling cash flow forecast (AR + promises - AP)")
    @GetMapping("/forecast")
    public R<CashFlowForecastVO> forecast(
            @RequestParam(required = false, defaultValue = "0") BigDecimal openingBalance) {
        return R.ok(service.forecast(openingBalance));
    }
}
