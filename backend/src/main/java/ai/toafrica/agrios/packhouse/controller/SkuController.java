package ai.toafrica.agrios.packhouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.packhouse.entity.Sku;
import ai.toafrica.agrios.packhouse.service.SkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "17 · Packhouse-SKU", description = "SKU 商品 (crop+variety+grade+spec 唯一)")
@RestController
@RequestMapping("/v1/packhouse/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @Operation(summary = "SKU 列表")
    @GetMapping
    public R<PageResult<Sku>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(skuService.page(code, grade, status, pq));
    }
}
