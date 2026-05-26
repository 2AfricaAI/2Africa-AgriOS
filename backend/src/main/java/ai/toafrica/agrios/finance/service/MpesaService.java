package ai.toafrica.agrios.finance.service;

/**
 * @deprecated Sprint 15.1 - 已被 {@link LoopService} 取代.
 *   Loop (NCBA) 是统一收单方,内部聚合 M-Pesa / Card / Bank, 不再需要直接对 Daraja.
 *   保留此空类避免 Spring 启动时找不到 Bean 报错;留作搜索时的告知文档.
 */
@Deprecated(forRemoval = true, since = "Sprint 15.1")
public class MpesaService {
    // 实现已迁移到 LoopService - 客户付款时仍可选 M-Pesa, 但走 Loop checkout.
}
