package ai.toafrica.agrios.org.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.org.entity.OrgTag;
import ai.toafrica.agrios.org.service.OrgTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "82 - ORG - Tags", description = "Cross-tree dimension tags")
@RestController
@RequestMapping("/v1/org/tags")
@RequiredArgsConstructor
public class OrgTagController {

    private final OrgTagService service;

    @Operation(summary = "List tags (filtered by category)")
    @GetMapping
    public R<List<OrgTag>> list(@RequestParam(required = false) String category,
                                @RequestParam(defaultValue = "false") boolean includeInactive) {
        return R.ok(service.listAll(category, includeInactive));
    }

    @Operation(summary = "Create a tag")
    @PostMapping
    public R<OrgTag> create(@RequestBody OrgTag req) {
        return R.ok(service.create(req));
    }

    @Operation(summary = "Delete a tag (detaches from all nodes first)")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "List tag ids attached to a node")
    @GetMapping("/by-node/{nodeId}")
    public R<List<Long>> byNode(@PathVariable Long nodeId) {
        return R.ok(service.tagIdsForNode(nodeId));
    }

    @Operation(summary = "List node ids carrying a tag")
    @GetMapping("/{tagId}/nodes")
    public R<List<Long>> nodesForTag(@PathVariable Long tagId) {
        return R.ok(service.nodeIdsForTag(tagId));
    }

    @Operation(summary = "Attach a tag to a node (idempotent)")
    @PostMapping("/{tagId}/attach/{nodeId}")
    public R<Void> attach(@PathVariable Long tagId, @PathVariable Long nodeId) {
        service.attach(nodeId, tagId);
        return R.ok();
    }

    @Operation(summary = "Detach a tag from a node")
    @DeleteMapping("/{tagId}/attach/{nodeId}")
    public R<Void> detach(@PathVariable Long tagId, @PathVariable Long nodeId) {
        service.detach(nodeId, tagId);
        return R.ok();
    }
}
