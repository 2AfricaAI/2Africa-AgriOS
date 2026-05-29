package ai.toafrica.agrios.packhouse.service.importer;

import ai.toafrica.agrios.framework.importer.ImportException;
import ai.toafrica.agrios.framework.importer.ImportTemplate;
import ai.toafrica.agrios.framework.importer.XlsxParser;
import ai.toafrica.agrios.master.entity.LocationWarehouse;
import ai.toafrica.agrios.master.mapper.LocationWarehouseMapper;
import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.entity.Sku;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.packhouse.mapper.SkuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 38g: special import for OPENING balances of finished-goods inventory.
 *
 * Normally inventory rows are created by PackingService (one row per pack). For
 * customers migrating from a legacy ERP we need to seed initial balances without
 * creating fake packing records.
 *
 * Columns:
 *   sku_code         - looked up to sku_id (required)
 *   warehouse_code   - looked up to location_id (required)
 *   grade            - 'A'/'B'/'C', defaults to 'A'
 *   qty_avail        - on-hand quantity (required, > 0)
 *   unit             - kg/box/etc (required)
 *   prod_date        - YYYY-MM-DD or blank
 *   expiry_date      - YYYY-MM-DD or blank
 *
 * De-dupe key: (sku_id, location_id, grade, expiry_date). Existing rows are
 * skipped (not summed) to keep the import idempotent.
 */
@Component("openingInventoryImportTemplate")
@RequiredArgsConstructor
public class OpeningInventoryImportTemplate implements ImportTemplate<Inventory> {

    private final InventoryMapper inventoryMapper;
    private final SkuMapper skuMapper;
    private final LocationWarehouseMapper warehouseMapper;

    private static final String[] HEADERS = {
            "sku_code", "warehouse_code", "grade",
            "qty_avail", "unit", "prod_date", "expiry_date"
    };
    private static final String[] SAMPLE = {
            "SKU-TOMATO-A-2KG", "AGW-01", "A",
            "150", "box", "2026-05-20", "2026-05-27"
    };

    @Override public String entityKey() { return "opening_inventory"; }
    @Override public String[] headers()  { return HEADERS; }
    @Override public String[] sampleRow() { return SAMPLE; }

    @Override
    public Inventory parseRow(Row row) {
        String skuCode = XlsxParser.requireStr(row, 0, "sku_code");
        String whCode = XlsxParser.requireStr(row, 1, "warehouse_code");
        BigDecimal qty = XlsxParser.decVal(row, 3, "qty_avail");
        if (qty == null || qty.signum() <= 0) {
            throw new ImportException("qty_avail", "must be a positive number");
        }
        String unit = XlsxParser.requireStr(row, 4, "unit");

        Inventory inv = new Inventory();
        // We use grade + temporary string fields here; ID resolution happens in
        // persistBatch where we have all SKU codes batched up.
        inv.setGrade(orDefault(XlsxParser.str(row, 2), "A"));
        inv.setQtyAvail(qty);
        inv.setQtyLocked(BigDecimal.ZERO);
        inv.setQtyInTransit(BigDecimal.ZERO);
        inv.setUnit(unit);
        inv.setProdDate(XlsxParser.dateVal(row, 5, "prod_date"));
        inv.setExpiryDate(XlsxParser.dateVal(row, 6, "expiry_date"));
        inv.setStatus("active");
        // Stash codes into transient session via the prod_date trick won't fly —
        // instead we abuse the unused batchId field to a -1 sentinel, and stash
        // codes on a parallel structure handed to persistBatch via a ThreadLocal.
        STAGED_ROW.get().add(new StagedRow(skuCode, whCode, inv));
        return inv;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersistOutcome persistBatch(List<Inventory> rows) {
        if (rows.isEmpty()) { STAGED_ROW.remove(); return new PersistOutcome(0, 0); }
        List<StagedRow> staged = STAGED_ROW.get();
        try {
            return doPersist(staged);
        } finally {
            STAGED_ROW.remove();
        }
    }

    private PersistOutcome doPersist(List<StagedRow> staged) {
        // Resolve SKU codes -> ids
        Set<String> skuCodes = new HashSet<>();
        Set<String> whCodes = new HashSet<>();
        for (StagedRow s : staged) { skuCodes.add(s.skuCode); whCodes.add(s.whCode); }

        Map<String, Long> skuMap = new HashMap<>();
        if (!skuCodes.isEmpty()) {
            for (Sku s : skuMapper.selectList(new LambdaQueryWrapper<Sku>().in(Sku::getCode, skuCodes))) {
                skuMap.put(s.getCode(), s.getId());
            }
        }
        Map<String, Long> whMap = new HashMap<>();
        if (!whCodes.isEmpty()) {
            for (LocationWarehouse w : warehouseMapper.selectList(
                    new LambdaQueryWrapper<LocationWarehouse>().in(LocationWarehouse::getCode, whCodes))) {
                whMap.put(w.getCode(), w.getId());
            }
        }

        int inserted = 0, skipped = 0;
        for (StagedRow s : staged) {
            Long skuId = skuMap.get(s.skuCode);
            if (skuId == null) {
                throw new ImportException("sku_code", "unknown sku: " + s.skuCode);
            }
            Long whId = whMap.get(s.whCode);
            if (whId == null) {
                throw new ImportException("warehouse_code", "unknown warehouse: " + s.whCode);
            }

            // Idempotency check: skip if a row with same (sku, location, grade, expiry) exists
            LambdaQueryWrapper<Inventory> q = new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getSkuId, skuId)
                    .eq(Inventory::getLocationId, whId)
                    .eq(Inventory::getGrade, s.inv.getGrade());
            if (s.inv.getExpiryDate() != null) {
                q.eq(Inventory::getExpiryDate, s.inv.getExpiryDate());
            } else {
                q.isNull(Inventory::getExpiryDate);
            }
            if (inventoryMapper.selectCount(q) > 0) { skipped++; continue; }

            s.inv.setSkuId(skuId);
            s.inv.setLocationId(whId);
            inventoryMapper.insert(s.inv);
            inserted++;
        }
        return new PersistOutcome(inserted, skipped);
    }

    private static String orDefault(String v, String d) { return (v == null || v.isBlank()) ? d : v; }

    // ------------------------------------------------------------
    // Per-import staging: code-resolution happens in persistBatch, so we need
    // to pass the raw codes from parseRow to persistBatch.  A ThreadLocal is
    // safe here because ImportRunner runs each upload on one request thread.
    // ------------------------------------------------------------
    private static final ThreadLocal<List<StagedRow>> STAGED_ROW =
            ThreadLocal.withInitial(java.util.ArrayList::new);

    private record StagedRow(String skuCode, String whCode, Inventory inv) {}
}
