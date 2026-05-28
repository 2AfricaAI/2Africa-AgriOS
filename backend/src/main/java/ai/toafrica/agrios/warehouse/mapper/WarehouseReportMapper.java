package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.vo.WarehouseReportVO.ItemMovement;
import ai.toafrica.agrios.warehouse.vo.WarehouseReportVO.LowStockItem;
import ai.toafrica.agrios.warehouse.vo.WarehouseReportVO.WarehouseStockSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Aggregation queries for warehouse reports (Sprint 22.9b).
 */
@Mapper
public interface WarehouseReportMapper {

    /** Doc counts: returns rows like {doc_type='inbound', status='confirmed', cnt=12} */
    @Select("""
            SELECT 'inbound' AS doc_type, status, COUNT(*) AS cnt
              FROM warehouse_inbound
              WHERE created_at >= #{from} AND created_at < #{to}
              GROUP BY status
            UNION ALL
            SELECT 'outbound', status, COUNT(*)
              FROM warehouse_outbound
              WHERE created_at >= #{from} AND created_at < #{to}
              GROUP BY status
            UNION ALL
            SELECT 'stocktake', status, COUNT(*)
              FROM warehouse_stocktake
              WHERE created_at >= #{from} AND created_at < #{to}
              GROUP BY status
            UNION ALL
            SELECT 'transfer', status, COUNT(*)
              FROM warehouse_transfer
              WHERE created_at >= #{from} AND created_at < #{to}
              GROUP BY status
            UNION ALL
            SELECT 'scrap', status, COUNT(*)
              FROM warehouse_scrap
              WHERE created_at >= #{from} AND created_at < #{to}
              GROUP BY status
            """)
    List<Map<String, Object>> docCountRows(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Top items by total IN/OUT qty within period from stock_log */
    @Select("""
            SELECT l.input_item_id AS input_item_id,
              ii.code             AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.unit             AS unit,
              SUM(l.qty)          AS total_qty,
              COUNT(*)            AS txn_count
            FROM input_stock_log l
            JOIN input_item ii ON l.input_item_id = ii.id
            WHERE l.direction = #{direction}
              AND l.created_at >= #{from} AND l.created_at < #{to}
            GROUP BY l.input_item_id, ii.code, ii.name, ii.name_en, ii.unit
            ORDER BY total_qty DESC
            LIMIT #{limit}
            """)
    List<ItemMovement> topMovement(@Param("direction") String direction,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to,
                                    @Param("limit") int limit);

    /** Items where available qty is below min_stock_qty */
    @Select("""
            SELECT s.input_item_id    AS input_item_id,
              ii.code                 AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.unit                 AS unit,
              s.warehouse_id          AS warehouse_id,
              w.name                  AS warehouse_name,
              s.qty_on_hand           AS qty_on_hand,
              s.qty_reserved          AS qty_reserved,
              (s.qty_on_hand - s.qty_reserved) AS qty_available,
              ii.min_stock_qty        AS min_stock_qty,
              (ii.min_stock_qty - (s.qty_on_hand - s.qty_reserved)) AS shortage_qty
            FROM input_stock s
            JOIN input_item ii        ON s.input_item_id = ii.id
            JOIN location_warehouse w ON s.warehouse_id  = w.id
            WHERE ii.min_stock_qty IS NOT NULL
              AND (s.qty_on_hand - s.qty_reserved) < ii.min_stock_qty
            ORDER BY shortage_qty DESC
            LIMIT 50
            """)
    List<LowStockItem> lowStockItems();

    /** Stock summary grouped by warehouse */
    @Select("""
            SELECT s.warehouse_id     AS warehouse_id,
              w.code                  AS warehouse_code,
              w.name                  AS warehouse_name,
              w.purpose               AS purpose,
              COUNT(DISTINCT s.input_item_id) AS item_count,
              COALESCE(SUM(s.qty_on_hand), 0) AS total_qty
            FROM input_stock s
            JOIN location_warehouse w ON s.warehouse_id = w.id
            GROUP BY s.warehouse_id, w.code, w.name, w.purpose
            ORDER BY total_qty DESC
            """)
    List<WarehouseStockSummary> stockByWarehouse();
}
