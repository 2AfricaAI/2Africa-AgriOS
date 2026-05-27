package ai.toafrica.agrios.procurement.mapper;

import ai.toafrica.agrios.procurement.entity.PurchaseOrderItem;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    @Select("""
            SELECT
              i.id                    AS id,
              i.po_id                 AS po_id,
              i.input_item_id         AS input_item_id,
              ii.name                 AS input_item_name,
              ii.code                 AS input_item_code,
              i.input_type            AS input_type,
              i.description           AS description,
              i.quantity              AS quantity,
              i.unit                  AS unit,
              i.unit_price            AS unit_price,
              i.amount                AS amount,
              i.received_qty          AS received_qty,
              i.remark                AS remark
              FROM purchase_order_item i
              LEFT JOIN input_item ii ON i.input_item_id = ii.id AND ii.deleted_at IS NULL
              WHERE i.po_id = #{poId}
              ORDER BY i.id ASC
            """)
    List<PurchaseOrderItemVO> findByPoId(@Param("poId") Long poId);

    /**
     * 按 inputType 查可用的 PO 行 - Sprint 17.7
     *   PO status 必须是 confirmed / partial_received / received (有效凭证)
     *   附带 PO code + supplier name 用于前端下拉展示
     */
    @Select("""
            SELECT
              i.id                AS id,
              i.po_id              AS po_id,
              o.code               AS po_code,
              o.order_date         AS po_order_date,
              s.id                 AS supplier_id,
              s.name               AS supplier_name,
              i.input_type         AS input_type,
              i.description        AS description,
              i.quantity           AS quantity,
              i.unit               AS unit,
              i.unit_price         AS unit_price,
              i.amount             AS amount,
              i.received_qty       AS received_qty
              FROM purchase_order_item i
              JOIN purchase_order o ON i.po_id = o.id AND o.deleted_at IS NULL
              JOIN supplier      s ON o.supplier_id = s.id AND s.deleted_at IS NULL
              WHERE i.input_type = #{inputType}
                AND o.status IN ('confirmed', 'partial_received', 'received')
              ORDER BY o.order_date DESC, i.id DESC
              LIMIT 200
            """)
    List<Map<String, Object>> findAvailableByInputType(@Param("inputType") String inputType);
}
