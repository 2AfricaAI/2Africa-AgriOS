package ai.toafrica.agrios.org.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.org.entity.OrgNode;
import ai.toafrica.agrios.org.service.OrgNodeService;
import ai.toafrica.agrios.org.vo.OrgNodeTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 51 -- OrgNode REST surface.
 *
 * <p>Decision #8 -- you decide; no external review gates these. Standard
 * REST + Operation annotations for Swagger so the frontend can self-serve.</p>
 */
@Slf4j
@Tag(name = "80 - ORG - Nodes", description = "Organization tree management")
@RestController
@RequestMapping("/v1/org/nodes")
@RequiredArgsConstructor
public class OrgNodeController {

    private final OrgNodeService service;

    @Operation(summary = "List all nodes (flat, ordered)")
    @GetMapping
    public R<List<OrgNode>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return R.ok(service.listAll(includeInactive));
    }

    @Operation(summary = "List nodes as a nested tree (single fetch for el-tree)")
    @GetMapping("/tree")
    public R<List<OrgNodeTreeVO>> tree(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return R.ok(service.tree(includeInactive));
    }

    @Operation(summary = "Get a node by id")
    @GetMapping("/{id}")
    public R<OrgNode> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "Get the subtree id list (inclusive)")
    @GetMapping("/{id}/subtree-ids")
    public R<List<Long>> subtreeIds(@PathVariable Long id) {
        return R.ok(service.subtreeIds(id));
    }

    @Operation(summary = "Create a node")
    @PostMapping
    public R<OrgNode> create(@RequestBody OrgNode req) {
        return R.ok(service.create(req));
    }

    @Operation(summary = "Update a node (parent move + rebuild supported)")
    @PutMapping("/{id}")
    public R<OrgNode> update(@PathVariable Long id, @RequestBody OrgNode req) {
        return R.ok(service.update(id, req));
    }

    @Operation(summary = "Delete a node (virtual only; physical types rejected)")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "Flip active flag (the only way to retire a physical node)")
    @PostMapping("/{id}/active")
    public R<OrgNode> setActive(@PathVariable Long id,
                                @RequestParam boolean active) {
        return R.ok(service.setActive(id, active));
    }
}
