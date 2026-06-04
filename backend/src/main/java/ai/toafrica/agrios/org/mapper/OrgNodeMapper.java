package ai.toafrica.agrios.org.mapper;

import ai.toafrica.agrios.org.entity.OrgNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Sprint 51 -- OrgNode CRUD + subtree helpers.
 *
 * <p>Subtree queries are the hot path for {@code @DataScope}; both
 * variants are exposed so the interceptor can pick the smaller payload
 * depending on the use case.</p>
 */
@Mapper
public interface OrgNodeMapper extends BaseMapper<OrgNode> {

    /**
     * Returns all descendant ids (inclusive) under {@code rootId}. Powered
     * by the {@code ancestors} LIKE prefix index, so it is O(subtree_size)
     * regardless of total tree size.
     */
    @Select({
        "<script>",
        "SELECT id FROM org_node",
        "WHERE deleted_at IS NULL",
        "  AND (id = #{rootId}",
        "       OR ancestors = #{rootIdStr}",
        "       OR ancestors LIKE CONCAT(#{rootIdStr}, '/%')",
        "       OR ancestors LIKE CONCAT('%/', #{rootIdStr}, '/%')",
        "       OR ancestors LIKE CONCAT('%/', #{rootIdStr}))",
        "</script>"
    })
    List<Long> selectSubtreeIds(@Param("rootId") Long rootId,
                                @Param("rootIdStr") String rootIdStr);

    /**
     * Returns true if the node has any direct active children. Used to
     * gate node deletion at the service layer (decision #3 -- can't
     * delete a DEPT with active children, must reassign first).
     */
    @Select("SELECT COUNT(*) > 0 FROM org_node "
          + "WHERE parent_id = #{parentId} AND deleted_at IS NULL AND active = 1")
    boolean hasActiveChildren(@Param("parentId") Long parentId);
}
