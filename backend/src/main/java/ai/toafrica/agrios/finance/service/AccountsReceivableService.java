package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.finance.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 应收账款台账 (AR) - Sprint 14.2.
 * 由 R-AR-01 / R-AR-02 行动清单规则触发依赖此数据.
 */
@Service
@RequiredArgsConstructor
public class AccountsReceivableService {

    private final PaymentMapper paymentMapper;

    /**
     * 按客户聚合应收 + 账龄分桶 (0-7 / 8-14 / 15-30 / 30+)
     */
    public List<Map<String, Object>> arAgingByCustomer() {
        return paymentMapper.arAgingByCustomer();
    }
}
