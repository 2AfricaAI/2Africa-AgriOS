package ai.toafrica.agrios.warehouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.service.InputStockService;
import ai.toafrica.agrios.warehouse.entity.WarehouseTransfer;
import ai.toafrica.agrios.warehouse.entity.WarehouseTransferItem;
import ai.toafrica.agrios.warehouse.mapper.WarehouseTransferItemMapper;
import ai.toafrica.agrios.warehouse.mapper.WarehouseTransferMapper;
import ai.toafrica.agrios.warehouse.vo.TransferDetailVO;
import ai.toafrica.agrios.warehouse.vo.TransferItemVO;
import ai.toafrica.agrios.warehouse.vo.TransferVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WarehouseTransferMapper transferMapper;
    private final WarehouseTransferItemMapper itemMapper;
    private final InputStockService stockService;
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PageResult<TransferVO> page(String status, PageQuery pq) {
        QueryWrapper<TransferVO> q = new QueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq("t.status", status.trim());
        q.orderByDesc("t.created_at");
        return PageResult.of(transferMapper.pageWithJoin(new Page<>(pq.getPage(), pq.getSize()), q));
    }

    public TransferDetailVO detail(Long id) {
        QueryWrapper<TransferVO> q = new QueryWrapper<>();
        q.eq("t.id", id);
        var records = transferMapper.pageWithJoin(new Page<>(1, 1), q).getRecords();
        if (records.isEmpty()) throw new BusinessException(R.NOT_FOUND, "Transfer not found");
        TransferDetailVO vo = new TransferDetailVO();
        vo.setHeader(records.get(0));
        vo.setItems(itemMapper.findByTransferId(id));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long fromWarehouseId, Long toWarehouseId, List<TransferItemInput> items, String remark) {
        if (fromWarehouseId.equals(toWarehouseId)) throw new BusinessException("Source and target warehouse cannot be the same");
        String today = LocalDateTime.now().format(YMD);
        int seq = transferMapper.countByDate(today) + 1;
        String code = String.format("TR-%s-%04d", today, seq);

        WarehouseTransfer tr = new WarehouseTransfer();
        tr.setCode(code);
        tr.setFromWarehouseId(fromWarehouseId);
        tr.setToWarehouseId(toWarehouseId);
        tr.setStatus("draft");
        tr.setRemark(remark);
        transferMapper.insert(tr);

        for (TransferItemInput ti : items) {
            WarehouseTransferItem item = new WarehouseTransferItem();
            item.setTransferId(tr.getId());
            item.setInputItemId(ti.inputItemId);
            item.setQty(ti.qty);
            itemMapper.insert(item);
        }
        log.info("[Transfer created] code={} from={} to={} items={}", code, fromWarehouseId, toWarehouseId, items.size());
        return tr.getId();
    }

    public record TransferItemInput(Long inputItemId, BigDecimal qty) {}

    /** 确认调拨: 源仓 -qty + 目标仓 +qty + 2条 log */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, Long operatorId) {
        WarehouseTransfer tr = transferMapper.selectById(id);
        if (tr == null) throw new BusinessException(R.NOT_FOUND, "Transfer not found");
        if (!"draft".equals(tr.getStatus())) throw new BusinessException("Only draft can be confirmed");

        List<WarehouseTransferItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<WarehouseTransferItem>().eq(WarehouseTransferItem::getTransferId, id));
        for (WarehouseTransferItem item : items) {
            // 源仓出库
            stockService.adjustStock(item.getInputItemId(), tr.getFromWarehouseId(),
                    item.getQty().negate(), "transfer_out", "warehouse_transfer", tr.getId(),
                    operatorId, "Transfer " + tr.getCode() + " out");
            // 目标仓入库
            stockService.adjustStock(item.getInputItemId(), tr.getToWarehouseId(),
                    item.getQty(), "transfer_in", "warehouse_transfer", tr.getId(),
                    operatorId, "Transfer " + tr.getCode() + " in");
        }
        tr.setStatus("confirmed");
        tr.setConfirmedBy(operatorId);
        tr.setConfirmedAt(LocalDateTime.now());
        transferMapper.updateById(tr);
        log.info("[Transfer confirmed] code={}", tr.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WarehouseTransfer tr = transferMapper.selectById(id);
        if (tr == null) throw new BusinessException(R.NOT_FOUND, "Transfer not found");
        if ("confirmed".equals(tr.getStatus())) throw new BusinessException("Cannot cancel confirmed transfer");
        tr.setStatus("cancelled");
        transferMapper.updateById(tr);
    }
}
