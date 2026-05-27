package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.production.dto.HarvestRecordForm;
import ai.toafrica.agrios.production.service.HarvestRecordService;
import ai.toafrica.agrios.production.vo.HarvestRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "13 · 生产-采收记录", description = "采收事件 - 创建时自动产生批次(全链路追溯主键)")
@RestController
@RequestMapping("/v1/production/harvests")
@RequiredArgsConstructor
public class HarvestRecordController {

    private final HarvestRecordService harvestService;

    @Operation(summary = "采收记录列表")
    @GetMapping
    public R<PageResult<HarvestRecordVO>> list(
            @RequestParam(required = false) Long plotId,
            @RequestParam(required = false) Long planId,
            @Parameter(description = "采收日期起 (含)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "采收日期止 (含)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            PageQuery pq) {
        return R.ok(harvestService.page(plotId, planId, dateFrom, dateTo, pq));
    }

    @Operation(summary = "新建采收记录 (自动产生 batch, 含工人移动端)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_LEADER') or hasAuthority('ROLE_WORKER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody HarvestRecordForm form) {
        return R.ok(harvestService.create(form));
    }

    @Operation(summary = "删除采收记录 (同时软删对应 batch)")
    @PreAuthorize("hasAuthority('ROLE_SU