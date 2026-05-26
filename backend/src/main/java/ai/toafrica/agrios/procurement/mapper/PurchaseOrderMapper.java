package ai.toafrica.agrios.procurement.mapper;

import ai.toafrica.agrios.procurement.entity.PurchaseOrder;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    @Select("""
            SELECT
              o.id, o.code,
              o.supplier_id, s.code AS supplier_code, s.name AS supplier_name, s.type AS supplier_type,
              o.order_date, o.expected_date,
              o.currency, o.fx_rate, o.total_amount, o.status,
              o.payment_status, o.paid_amount, o.due_date,
              (SELECT COUNT(*) FROM purchase_order_item WHERE po_id = o.id) AS item_count,
              o.remark, o.created_at, o.updated_at
              FROM purchase_order o
              LEFT JOIN supplier s ON o.supplier_id = s.id
              ${ew.customSqlSegment}
            """)
    IPage<PurchaseOrderVO> pageWithJoin(Page<PurchaseOrderVO> page,
                                       @Param("ew") QueryWrapper<PurchaseOrderVO> wrapper);

    @Select("SELECT COUNT(*) FROM purchase_order WHERE order_date = #{date} AND deleted_at IS NULL")
    int countByDate(@Param("date") LocalDate date);
}
