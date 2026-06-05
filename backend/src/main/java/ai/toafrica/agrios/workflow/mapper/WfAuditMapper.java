package ai.toafrica.agrios.workflow.mapper;

import ai.toafrica.agrios.workflow.entity.WfAudit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Sprint 52 -- INSERT only at the app layer; DB triggers reject
 * UPDATE/DELETE. Custom queries via LambdaQueryWrapper.
 */
@Mapper
public interface WfAuditMapper extends BaseMapper<WfAudit> {}
