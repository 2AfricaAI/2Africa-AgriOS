package ai.toafrica.agrios.procurement.service;

import ai.toafrica.agrios.procurement.mapper.VendorPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 应付账款台账 (AP) - Sprint 17.5 (镜像 AR).
 *   R-AP-01 应付逾期规则依赖此数据.
 */
@Service
@RequiredArgsConstructor
public class AccountsPayableService {

    private final VendorPaymentMapper vendorPaymentMapper;

    /**
     * 按供应商聚合应付 + 账龄分桶 (0-7 / 8-14 / 15-30 / 30+)
     */
    public List<Map<String, Object>> apAgingBySupplier() {
        return vendorPaymentMapper.apAgingBySupplier();
    }
}
