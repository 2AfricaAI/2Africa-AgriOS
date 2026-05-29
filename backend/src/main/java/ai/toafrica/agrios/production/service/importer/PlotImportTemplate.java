package ai.toafrica.agrios.production.service.importer;

import ai.toafrica.agrios.framework.importer.ImportTemplate;
import ai.toafrica.agrios.framework.importer.XlsxParser;
import ai.toafrica.agrios.production.entity.Plot;
import ai.toafrica.agrios.production.mapper.PlotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sprint 38e: Excel import for plots.
 *
 *   Columns: code, name, area_mu, location, soil_type, irrigation, status, remark
 *
 * De-dupe key: code. Existing codes are skipped.
 */
@Component("plotImportTemplate")
@RequiredArgsConstructor
public class PlotImportTemplate implements ImportTemplate<Plot> {

    private final PlotMapper plotMapper;

    private static final String[] HEADERS = {
            "code", "name", "area_mu", "location",
            "soil_type", "irrigation", "status", "remark"
    };
    private static final String[] SAMPLE = {
            "P-01-A", "Plot 01 - East Field", "12.5", "GPS:-1.2921,36.8219",
            "loam", "drip", "active", "Original tomato field"
    };

    @Override public String entityKey() { return "plots"; }
    @Override public String[] headers()  { return HEADERS; }
    @Override public String[] sampleRow() { return SAMPLE; }

    @Override
    public Plot parseRow(Row row) {
        Plot p = new Plot();
        p.setCode(XlsxParser.requireStr(row, 0, "code"));
        p.setName(XlsxParser.requireStr(row, 1, "name"));
        p.setAreaMu(XlsxParser.decVal(row, 2, "area_mu"));
        p.setLocation(XlsxParser.str(row, 3));
        p.setSoilType(XlsxParser.str(row, 4));
        p.setIrrigation(XlsxParser.str(row, 5));
        String status = XlsxParser.str(row, 6);
        p.setStatus(status == null ? "active" : status);
        p.setRemark(XlsxParser.str(row, 7));
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersistOutcome persistBatch(List<Plot> rows) {
        if (rows.isEmpty()) return new PersistOutcome(0, 0);

        Set<String> codes = new HashSet<>();
        for (Plot p : rows) codes.add(p.getCode());

        Set<String> existing = new HashSet<>();
        if (!codes.isEmpty()) {
            List<Plot> dup = plotMapper.selectList(
                    new LambdaQueryWrapper<Plot>().in(Plot::getCode, codes));
            for (Plot e : dup) existing.add(e.getCode());
        }

        int inserted = 0, skipped = 0;
        for (Plot p : rows) {
            if (existing.contains(p.getCode())) { skipped++; continue; }
            plotMapper.insert(p);
            inserted++;
        }
        return new PersistOutcome(inserted, skipped);
    }
}
