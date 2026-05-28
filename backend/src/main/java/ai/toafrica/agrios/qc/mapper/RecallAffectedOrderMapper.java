package ai.toafrica.agrios.qc.mapper;

import ai.toafrica.agrios.qc.entity.RecallAffectedOrder;
import ai.toafrica.agrios.qc.vo.RecallDetailVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecallAffectedOrderMapper extends BaseMapper<RecallAffectedOrder> {

    @Select("""
            SELECT
              rao.id, rao.order_id, rao.order_code,
              rao.customer_id, rao.customer_name,
              rao.qty, rao.unit,
              rao.delivered_at, rao.notified_at,
              rao.notified_by_id, nu.nickname AS notified_by_name
            FROM recall_affected_order rao
            LEFT JOIN sys_user nu ON rao.notified_by_id = nu.id
            WHERE rao.recall_id = #{recallId}
            ORDER BY rao.id
            """)
    List<RecallDetailVO.AffectedOrder> findByRecallId(@Param("recallId") Long recallId);
}
