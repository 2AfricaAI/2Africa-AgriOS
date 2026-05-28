package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.production.dto.PlotDTO;
import ai.toafrica.agrios.production.entity.Plot;
import ai.toafrica.agrios.production.mapper.PlotMapper;
import ai.toafrica.agrios.production.service.PlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "10 · Production-Plots", description = "Plot management")
@RestController
@RequestMapping("/v1/production/plots")
@RequiredArgsConstructor
public class PlotController {

    private final PlotService plotService;

    @Operation(summary = "Plot list (paginated)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_LEADER') or hasAuthority('ROLE_PACKHOUSE') or hasAuthority('ROLE_SALES')")
    @GetMapping
    public R<PageResult<PlotMapper.PlotVO>> list(PlotMapper.PlotQueryVO q, PageQuery pq) {
        return R.ok(plotService.page(q, pq));
    }

    @Operation(summary = "Plot detail")
    @GetMapping("/{id}")
    public R<Plot> get(@PathVariable Long id) {
        return R.ok(plotService.detail(id));
    }

    @Operation(summary = "Plot stats over the last 30 days")
    @GetMapping("/{id}/stats")
    public R<PlotMapper.PlotStatVO> stats(@PathVariable Long id) {
        return R.ok(plotService.stats(id));
    }

    @Operation(summary = "Create plot")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PlotDTO dto) {
        return R.ok(plotService.create(dto));
    }

    @Operation(summary = "Edit plot")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlotDTO dto) {
        plotService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "Disable / enable / fallow")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        plotService.toggleStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Delete plot (soft delete)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        plotService.delete(id);
        return R.ok();
    }
}
