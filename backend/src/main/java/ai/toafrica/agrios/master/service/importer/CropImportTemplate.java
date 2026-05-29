package ai.toafrica.agrios.master.service.importer;

import ai.toafrica.agrios.framework.importer.ImportTemplate;
import ai.toafrica.agrios.framework.importer.XlsxParser;
import ai.toafrica.agrios.master.entity.Crop;
import ai.toafrica.agrios.master.mapper.CropMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sprint 38: Excel import for the crop master table.
 *
 *   Columns: code, name, category, unit, cycle_days, shelf_life_days, remark
 *
 * De-dupe key is {@code code}.  Existing codes are skipped, not failed.
 */
@Component("cropImportTemplate")
@RequiredArgsConstructor
public class CropImportTemplate implements ImportTemplate<Crop> {

    private final CropMapper cropMapper;

    private static final String[] HEADERS = {
            "code", "name", "category", "unit", "cycle_days", "shelf_life_days", "remark"
    };
    private static final String[] SAMPLE = {
            "TOMATO", "Tomato", "fruit", "kg", "90", "7", "Common red tomato"
    };

    @Override public String entityKey() { return "crops"; }
    @Override public String[] headers()  { return HEADERS; }
    @Override public String[] sampleRow() { return SAMPLE; }

    @Override
    public Crop parseRow(Row row) {
        Crop c = new Crop();
        c.setCode(XlsxParser.requireStr(row, 0, "code").toUpperCase());
        c.setName(XlsxParser.requireStr(row, 1, "name"));
        c.setCategory(XlsxParser.str(row, 2));
        c.setUnit(XlsxParser.str(row, 3));
        c.setCycleDays(XlsxParser.intVal(row, 4, "cycle_days"));
        c.setShelfLifeDays(XlsxParser.intVal(row, 5, "shelf_life_days"));
        c.setRemark(XlsxParser.str(row, 6));
        c.setStatus(1);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersistOutcome persistBatch(List<Crop> rows) {
        if (rows.isEmpty()) return new PersistOutcome(0, 0);

        // Single query for all existing codes -> avoid N+1
        Set<String> codes = new HashSet<>();
        for (Crop c : rows) codes.add(c.getCode());
        Set<String> existing = new HashSet<>();
        if (!codes.isEmpty()) {
            List<Crop> existingRows = cropMapper.selectList(
                    new LambdaQueryWrapper<Crop>().in(Crop::getCode, codes));
            for (Crop e : existingRows) existing.add(e.getCode());
        }

        int inserted = 0, skipped = 0;
        for (Crop c : rows) {
            if (existing.contains(c.getCode())) { skipped++; continue; }
            cropMapper.insert(c);
            inserted++;
        }
        return new PersistOutcome(inserted, skipped);
    }
}
