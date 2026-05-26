package ai.toafrica.agrios.packhouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.master.entity.Crop;
import ai.toafrica.agrios.master.entity.PackagingSpec;
import ai.toafrica.agrios.master.entity.Variety;
import ai.toafrica.agrios.master.mapper.CropMapper;
import ai.toafrica.agrios.master.mapper.PackagingSpecMapper;
import ai.toafrica.agrios.master.mapper.VarietyMapper;
import ai.toafrica.agrios.packhouse.entity.Sku;
import ai.toafrica.agrios.packhouse.mapper.SkuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkuService {

    private final SkuMapper skuMapper;
    private final CropMapper cropMapper;
    private final VarietyMapper varietyMapper;
    private final PackagingSpecMapper packagingSpecMapper;

    public PageResult<Sku> page(String code, String grade, Integer status, PageQuery pq) {
        LambdaQueryWrapper<Sku> q = new LambdaQueryWrapper<>();
        if (code != null && !code.isBlank()) q.like(Sku::getCode, code.trim());
        if (grade != null && !grade.isBlank()) q.eq(Sku::getGrade, grade.trim());
        if (status != null) q.eq(Sku::getStatus, status);
        q.orderByDesc(Sku::getId);
        Page<Sku> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(skuMapper.selectPage(p, q));
    }

    /**
     * 找或建 SKU - 包装单创建时用
     * 唯一维度: (cropId, varietyId, grade, specId)
     */
    public Sku findOrCreate(Long cropId, Long varietyId, String grade, Long specId) {
        Sku existing = skuMapper.findByDims(cropId, varietyId, grade, specId);
        if (existing != null) return existing;

        Crop crop = cropMapper.selectById(cropId);
        PackagingSpec spec = packagingSpecMapper.selectById(specId);
        Variety variety = varietyId != null ? varietyMapper.selectById(varietyId) : null;

        Sku s = new Sku();
        // code: SKU-{cropCode}-{varietyCode or NA}-{grade}-{specCode}
        String vCode = variety != null ? variety.getCode() : "NA";
        s.setCode("SKU-" + crop.getCode() + "-" + vCode + "-" + grade + "-" + spec.getCode());
        // 显示名: "Tomato Cherry · A · 250g Clear Punnet"
        String vName = variety != null ? (" " + variety.getName()) : "";
        s.setName(crop.getName() + vName + " · " + grade + " · " + spec.getName());
        s.setCropId(cropId);
        s.setVarietyId(varietyId);
        s.setGrade(grade);
        s.setSpecId(specId);
        s.setUnit("pack");
        s.setStatus(1);
        skuMapper.insert(s);
        log.info("[SKU 自动创建] {} → {}", s.getCode(), s.getName());
        return s;
    }
}
