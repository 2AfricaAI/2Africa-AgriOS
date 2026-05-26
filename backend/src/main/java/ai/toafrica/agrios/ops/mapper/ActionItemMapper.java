package ai.toafrica.agrios.ops.mapper;

import ai.toafrica.agrios.ops.entity.ActionItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * action_item 的标准 mapper.
 *
 * upsert 逻辑放在 service 层用 selectOne + insert/updateById 实现, 避免自定义 SQL.
 */
@Mapper
public interface ActionItemMapper extends BaseMapper<ActionItem> {
}
