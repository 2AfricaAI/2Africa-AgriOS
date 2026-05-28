package ai.toafrica.agrios.qc.mapper;

import ai.toafrica.agrios.qc.entity.Complaint;
import ai.toafrica.agrios.qc.vo.ComplaintVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {

    @Select("""
            SELECT
              c.id, c.code, c.reported_at,
              c.customer_id, cu.name AS customer_name,
              c.order_id, o.code AS order_code,
              c.batch_id, b.code AS batch_code,
              c.sku_id, sk.code AS sku_code, sk.name AS sku_name,
              c.category, c.severity, c.channel, c.description, c.photo_ids,
              c.status, c.resolution, c.resolution_amount,
              c.reported_by_id, ru.nickname AS reported_by_name,
              c.resolved_at, c.resolved_by_id, rsu.nickname AS resolved_by_name,
              c.recall_id, rc.code AS recall_code,
              c.created_at, c.updated_at
            FROM complaint c
            LEFT JOIN customer     cu  ON c.customer_id    = cu.id
            LEFT JOIN sales_order  o   ON c.order_id       = o.id
            LEFT JOIN batch        b   ON c.batch_id       = b.id
            LEFT JOIN sku          sk  ON c.sku_id         = sk.id
            LEFT JOIN sys_user     ru  ON c.reported_by_id = ru.id
            LEFT JOIN sys_user     rsu ON c.resolved_by_id = rsu.id
            LEFT JOIN recall       rc  ON c.recall_id      = rc.id
            ${ew.customSqlSegment}
            """)
    IPage<ComplaintVO> pageWithJoin(Page<ComplaintVO> page,
                                    @Param("ew") QueryWrapper<ComplaintVO> wrapper);

    @Select("""
            SELECT COUNT(*) FROM complaint
            WHERE DATE(reported_at) = #{day}
            """)
    int countByDate(@Param("day") LocalDate day);
}
