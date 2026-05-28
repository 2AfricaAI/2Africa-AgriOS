package ai.toafrica.agrios.packhouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.master.entity.Crop;
import ai.toafrica.agrios.master.entity.PackagingSpec;
import ai.toafrica.agrios.master.entity.Variety;
import ai.toafrica.agrios.master.mapper.CropMapper;
import ai.toafrica.agrios.master.mapper.PackagingSpecMapper;
import ai.toafrica.agrios.master.mapper.VarietyMapper;
import ai.toafrica.agrios.packhouse.dto.PackingForm;
import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.entity.InventoryAdjustLog;
import ai.toafrica.agrios.packhouse.entity.Packing;
import ai.toafrica.agrios.packhouse.entity.Sku;
import ai.toafrica.agrios.packhouse.mapper.InventoryAdjustLogMapper;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.packhouse.mapper.PackingMapper;
import ai.toafrica.agrios.packhouse.vo.PackingRow;
import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.mapper.BatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 包装单 - Packhouse 业务核心
 *
 * 创建一张包装单时,事务完成 5 件事:
 *   1. 校验源批次有足够余量
 *   2. 找或建 SKU (复用 SkuService)
 *   3. 插入 packing 记录
 *   4. 扣减 batch.qty_remain_kg, 若归零 batch.status → 'packed'
 *   5. inventory upsert (按 sku+batch+grade+location 维度) + 写 adjust_log
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackingService {

    private final PackingMapper packingMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryAdjustLogMapper adjustLogMapper;
    private final BatchMapper batchMapper;
    private final PackagingSpecMapper specMapper;
    private final CropMapper cropMapper;
    private final VarietyMapper varietyMapper;
    private final SkuService skuService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================================================
    // 列表
    // ============================================================
    public PageResult<PackingRow> page(Long batchId, Long skuId, Long locationId,
                                        String grade, PageQuery pq) {
        QueryWrapper<PackingRow> q = new QueryWrapper<>();
        if (batchId != null) q.eq("pk.batch_id", batchId);
        if (skuId != null) q.eq("pk.sku_id", skuId);
        if (locationId != null) q.eq("pk.location_id", locationId);
        if (grade != null && !grade.isBlank()) q.eq("pk.grade", grade.trim());
        q.orderByDesc("pk.packed_at").orderByDesc("pk.id");
        Page<PackingRow> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(packingMapper.pageWithJoin(p, q));
    }

    // ============================================================
    // 创建 - 核心事务
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long create(PackingForm form) {
        // 1) 取 batch + spec
        Batch batch = batchMapper.selectById(form.getBatchId());
        if (batch == null) throw new BusinessException("Batch not found");
        if (!"pending".equals(batch.getStatus()) && !"processing".equals(batch.getStatus())) {
            throw new BusinessException("Batch current status does not allow packing: " + batch.getStatus());
        }

        PackagingSpec spec = specMapper.selectById(form.getSpecId());
        if (spec == null) throw new BusinessException("Packaging spec not found");

        // 2) 计算净重 (如果前端没传, 自动按规格 × 件数)
        BigDecimal netKg = form.getNetWeightKg();
        if (netKg == null) {
            netKg = spec.getUnitNetKg().multiply(BigDecimal.valueOf(form.getQtyUnits()));
        }

        // 3) 校验余量
        if (batch.getQtyRemainKg().compareTo(netKg) < 0) {
            throw new BusinessException(String.format(
                    "Batch remaining %s kg is insufficient to pack %s kg",
                    batch.getQtyRemainKg().toPlainString(), netKg.toPlainString()));
        }

        // 4) 找或建 SKU
        Sku sku = skuService.findOrCreate(
                batch.getCropId(), batch.getVarietyId(), form.getGrade(), form.getSpecId());

        // 5) 生成包装单号
        LocalDate packedDate = form.getPackedAt().toLocalDate();
        int seq = packingMapper.countByDate(packedDate) + 1;
        String code = String.format("PK-%s-%03d", packedDate.format(YMD), seq);

        // 6) 插入 packing
        Packing pk = new Packing();
        pk.setCode(code);
        pk.setBatchId(batch.getId());
        pk.setGrade(form.getGrade());
        pk.setSpecId(form.getSpecId());
        pk.setSkuId(sku.getId());
        pk.setQtyUnits(form.getQtyUnits());
        pk.setNetWeightKg(netKg);
        pk.setLocationId(form.getLocationId());
        pk.setPackedAt(form.getPackedAt());
        pk.setOperatorId(SecurityUtil.currentUserId());
        pk.setRemark(form.getRemark());
        packingMapper.insert(pk);

        // 7) 扣 batch 余量,可能进入 packed/processing 状态
        BigDecimal newRemain = batch.getQtyRemainKg().subtract(netKg);
        batch.setQtyRemainKg(newRemain);
        if (newRemain.compareTo(BigDecimal.ZERO) <= 0) {
            batch.setStatus("packed");
        } else if ("pending".equals(batch.getStatus())) {
            batch.setStatus("processing");
        }
        batchMapper.updateById(batch);

        // 8) inventory upsert (按 sku + batch + grade + location 维度)
        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, sku.getId())
                .eq(Inventory::getBatchId, batch.getId())
                .eq(Inventory::getGrade, form.getGrade())
                .eq(Inventory::getLocationId, form.getLocationId()));

        BigDecimal qtyBefore;
        BigDecimal qtyAfter;
        BigDecimal qtyChange = BigDecimal.valueOf(form.getQtyUnits());  // 件数

        if (inv == null) {
            qtyBefore = BigDecimal.ZERO;
            qtyAfter = qtyChange;
            inv = new Inventory();
            inv.setSkuId(sku.getId());
            inv.setBatchId(batch.getId());
            inv.setGrade(form.getGrade());
            inv.setLocationId(form.getLocationId());
            inv.setQtyAvail(qtyAfter);
            inv.setQtyLocked(BigDecimal.ZERO);
            inv.setQtyInTransit(BigDecimal.ZERO);
            inv.setUnit("pack");
            inv.setProdDate(batch.getHarvestDate());
            // Sprint 26 / FEFO: expiry_date = pack_date + shelf_life
            // Variety override > Crop default. Null = no expiry tracking.
            inv.setExpiryDate(resolveExpiryDate(batch.getCropId(), batch.getVarietyId(), packedDate));
            inv.setStatus("normal");
            inv.setLastOpAt(LocalDateTime.now());
            inventoryMapper.insert(inv);
        } else {
            qtyBefore = inv.getQtyAvail();
            qtyAfter = qtyBefore.add(qtyChange);
            inv.setQtyAvail(qtyAfter);
            inv.setLastOpAt(LocalDateTime.now());
            inventoryMapper.updateById(inv);
        }

        // 9) Write adjust_log
        InventoryAdjustLog adjustLog = new InventoryAdjustLog();
        adjustLog.setInventoryId(inv.getId());
        adjustLog.setAdjustType("in");
        adjustLog.setReasonCode("packing");
        adjustLog.setQtyBefore(qtyBefore);
        adjustLog.setQtyChange(qtyChange);
        adjustLog.setQtyAfter(qtyAfter);
        adjustLog.setFieldName("qty_avail");
        adjustLog.setRefType("packing");
        adjustLog.setRefId(pk.getId());
        adjustLog.setRemark("Packing " + code);
        adjustLog.setOperatorId(SecurityUtil.currentUserId());
        adjustLogMapper.insert(adjustLog);

        PackingService.log.info("[Packing done] code={} batch={} sku={} units={} netKg={} newRemain={}",
                code, batch.getCode(), sku.getCode(),
                form.getQtyUnits(), netKg.toPlainString(), newRemain.toPlainString());

        return pk.getId();
    }

    /**
     * Sprint 26 / FEFO. Resolve shelf_life_days for a crop+variety, then return
     * pack_date + shelf_life as the expiry date.
     */
    private LocalDate resolveExpiryDate(Long cropId, Long varietyId, LocalDate packDate) {
        Integer days = null;
        if (varietyId != null) {
            Variety v = varietyMapper.selectById(varietyId);
            if (v != null && v.getShelfLifeDays() != null) days = v.getShelfLifeDays();
        }
        if (days == null && cropId != null) {
            Crop c = cropMapper.selectById(cropId);
            if (c != null && c.getShelfLifeDays() != null) days = c.getShelfLifeDays();
        }
        return (days == null || packDate == null) ? null : packDate.plusDays(days);
    }
}
