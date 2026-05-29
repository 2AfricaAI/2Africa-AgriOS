package ai.toafrica.agrios.sales.service.importer;

import ai.toafrica.agrios.framework.importer.ImportTemplate;
import ai.toafrica.agrios.framework.importer.XlsxParser;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sprint 38f: Excel import for customers.
 *
 * Columns: code (optional auto), name, type, contact_name, contact_phone,
 *          credit_level, credit_days, payment_terms, remark
 *
 * De-dupe key: code (skipped if exists). If code is blank, auto-generated.
 */
@Component("customerImportTemplate")
@RequiredArgsConstructor
public class CustomerImportTemplate implements ImportTemplate<Customer> {

    private final CustomerMapper customerMapper;

    private static final String[] HEADERS = {
            "code", "name", "type", "contact_name", "contact_phone",
            "credit_level", "credit_days", "payment_terms", "remark"
    };
    private static final String[] SAMPLE = {
            "CUS-00001", "Acme Retail Ltd", "supermarket",
            "John Doe", "+254712345678", "A", "30", "Net 30",
            "Imported from legacy ERP"
    };

    @Override public String entityKey() { return "customers"; }
    @Override public String[] headers()  { return HEADERS; }
    @Override public String[] sampleRow() { return SAMPLE; }

    @Override
    public Customer parseRow(Row row) {
        Customer c = new Customer();
        c.setCode(XlsxParser.str(row, 0));  // optional - will auto-gen if blank
        c.setName(XlsxParser.requireStr(row, 1, "name"));
        String type = XlsxParser.str(row, 2);
        c.setType(type == null ? "other" : type);
        c.setContactName(XlsxParser.str(row, 3));
        c.setContactPhone(XlsxParser.str(row, 4));
        String level = XlsxParser.str(row, 5);
        c.setCreditLevel(level == null ? "C" : level);
        c.setCreditDays(XlsxParser.intVal(row, 6, "credit_days"));
        c.setPaymentTerms(XlsxParser.str(row, 7));
        c.setRemark(XlsxParser.str(row, 8));
        c.setStatus("active");
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersistOutcome persistBatch(List<Customer> rows) {
        if (rows.isEmpty()) return new PersistOutcome(0, 0);

        // Pre-collect declared codes so we can de-dupe in one query.
        Set<String> declaredCodes = new HashSet<>();
        for (Customer c : rows) {
            if (c.getCode() != null && !c.getCode().isBlank()) declaredCodes.add(c.getCode());
        }
        Set<String> existing = new HashSet<>();
        if (!declaredCodes.isEmpty()) {
            List<Customer> dup = customerMapper.selectList(
                    new LambdaQueryWrapper<Customer>().in(Customer::getCode, declaredCodes));
            for (Customer e : dup) existing.add(e.getCode());
        }

        // Find current max numeric code suffix for auto-gen (CUS-NNNNN).
        long nextSeq = nextSequence();

        int inserted = 0, skipped = 0;
        for (Customer c : rows) {
            if (c.getCode() != null && !c.getCode().isBlank() && existing.contains(c.getCode())) {
                skipped++; continue;
            }
            if (c.getCode() == null || c.getCode().isBlank()) {
                c.setCode(String.format("CUS-%05d", nextSeq++));
            }
            customerMapper.insert(c);
            inserted++;
        }
        return new PersistOutcome(inserted, skipped);
    }

    private long nextSequence() {
        Customer latest = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .likeRight(Customer::getCode, "CUS-")
                        .orderByDesc(Customer::getCode)
                        .last("LIMIT 1"));
        if (latest == null || latest.getCode() == null) return 1L;
        try {
            return Long.parseLong(latest.getCode().substring(4)) + 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}
