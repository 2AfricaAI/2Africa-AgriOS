package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.qc.mapper.TraceMapper;
import ai.toafrica.agrios.qc.vo.TraceVO;
import ai.toafrica.agrios.qc.vo.TraceVO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批次完整追溯链聚合服务 (Sprint 25 / Phase 5).
 *
 * 完整链:
 *   PO 入库 (Inbound) → 农事(spray/fertilize) → 采收 → 批次 → 包装 → 销售订单
 *
 * 入参: batchCode (e.g., B-20260527-P-002-01)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTraceService {

    private final TraceMapper traceMapper;

    public TraceVO trace(String batchCode) {
        BatchNode batch = traceMapper.findBatch(batchCode);
        if (batch == null) throw new BusinessException(R.NOT_FOUND, "Batch not found: " + batchCode);
        Long batchId = batch.getId();

        TraceVO vo = new TraceVO();
        vo.setBatch(batch);
        vo.setHarvest(traceMapper.findHarvest(batchId));
        vo.setPlan(traceMapper.findPlan(batchId));
        vo.setPlot(traceMapper.findPlot(batchId));

        // 上游农事
        if (vo.getPlan() != null) {
            List<ActivityNode> acts = traceMapper.findActivities(vo.getPlan().getId());
            Set<Long> allInputItemIds = new HashSet<>();
            for (ActivityNode a : acts) {
                List<InputUsed> inputs = traceMapper.findActivityInputs(a.getId());
                a.setInputs(inputs);
                for (InputUsed iu : inputs) allInputItemIds.add(iu.getInputItemId());
            }
            vo.setActivities(acts);

            // 追溯到 PO 入库
            if (!allInputItemIds.isEmpty()) {
                List<InboundNode> inbounds = traceMapper.findInboundsForInputs(List.copyOf(allInputItemIds));
                for (InboundNode ib : inbounds) {
                    ib.setItems(traceMapper.findInboundItems(ib.getId()));
                }
                vo.setInbounds(inbounds);
            }
        }

        // 下游 packing + orders
        vo.setPackings(traceMapper.findPackings(batchId));
        vo.setOrders(traceMapper.findOrders(batchId));

        log.info("[Trace] batch={} activities={} inbounds={} packings={} orders={}",
                batchCode,
                vo.getActivities() != null ? vo.getActivities().size() : 0,
                vo.getInbounds() != null ? vo.getInbounds().size() : 0,
                vo.getPackings() != null ? vo.getPackings().size() : 0,
                vo.getOrders() != null ? vo.getOrders().size() : 0);
        return vo;
    }
}
