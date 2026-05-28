package ai.toafrica.agrios.qc.mapper;

import ai.toafrica.agrios.qc.vo.TraceVO.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TraceMapper {

    @Select("""
            SELECT b.id, b.code, b.status, b.qty_kg, DATE(b.created_at) AS created_date,
              v.name AS variety_name, c.name AS crop_name
            FROM batch b
            LEFT JOIN variety v ON b.variety_id = v.id
            LEFT JOIN crop c    ON v.crop_id = c.id
            WHERE b.code = #{batchCode}
            """)
    BatchNode findBatch(@Param("batchCode") String batchCode);

    @Select("""
            SELECT h.id, h.code, h.harvest_date, h.qty_kg,
              u.nickname AS operator_name, h.location_gps
            FROM batch b
            JOIN harvest_record h ON b.harvest_record_id = h.id
            LEFT JOIN sys_user u  ON h.operator_id = u.id
            WHERE b.id = #{batchId}
            """)
    HarvestNode findHarvest(@Param("batchId") Long batchId);

    @Select("""
            SELECT p.id, p.code, p.planned_sow_date, p.planned_harvest_date, p.status
            FROM batch b
            JOIN harvest_record h ON b.harvest_record_id = h.id
            JOIN planting_plan p  ON h.plan_id = p.id
            WHERE b.id = #{batchId}
            """)
    PlanNode findPlan(@Param("batchId") Long batchId);

    @Select("""
            SELECT pl.id, pl.code, pl.name, pl.region_name, pl.area_ha
            FROM batch b
            JOIN harvest_record h ON b.harvest_record_id = h.id
            JOIN planting_plan p  ON h.plan_id = p.id
            JOIN plot pl          ON p.plot_id = pl.id
            WHERE b.id = #{batchId}
            """)
    PlotNode findPlot(@Param("batchId") Long batchId);

    @Select("""
            SELECT a.id, a.activity_type, a.occur_date,
              u.nickname AS operator_name
            FROM activity a
            LEFT JOIN sys_user u ON a.operator_id = u.id
            WHERE a.plan_id = #{planId}
              AND a.activity_type IN ('spray','fertilize','sow','water','weed','prune')
            ORDER BY a.occur_date ASC
            """)
    List<ActivityNode> findActivities(@Param("planId") Long planId);

    @Select("""
            SELECT ai.input_id AS input_item_id,
              ii.code AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.active_ingredient, ii.phi_days,
              ai.qty, ai.unit
            FROM activity_input ai
            JOIN input_item ii ON ai.input_id = ii.id
            WHERE ai.activity_id = #{activityId}
            ORDER BY ai.id
            """)
    List<InputUsed> findActivityInputs(@Param("activityId") Long activityId);

    /**
     * 给定一组 input_item_id (该 plan 用过的), 找到他们来自的 inbound 单
     */
    @Select("""
            <script>
            SELECT DISTINCT wi.id, wi.code, wi.source_type, wi.source_id,
              po.code AS source_code,
              DATE(wi.confirmed_at) AS confirmed_at
            FROM warehouse_inbound wi
            JOIN warehouse_inbound_item wii ON wii.inbound_id = wi.id
            LEFT JOIN purchase_order po ON wi.source_type = 'po_receive' AND wi.source_id = po.id
            WHERE wi.status = 'confirmed'
              AND wii.input_item_id IN
              <foreach collection='inputItemIds' item='id' open='(' separator=',' close=')'>
                #{id}
              </foreach>
            ORDER BY wi.confirmed_at DESC
            </script>
            """)
    List<InboundNode> findInboundsForInputs(@Param("inputItemIds") List<Long> inputItemIds);

    @Select("""
            SELECT wii.input_id AS input_item_id,
              ii.code AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              COALESCE(wii.actual_qty, wii.expected_qty) AS qty
            FROM warehouse_inbound_item wii
            JOIN input_item ii ON wii.input_id = ii.id
            WHERE wii.inbound_id = #{inboundId}
            """)
    List<InputReceived> findInboundItems(@Param("inboundId") Long inboundId);

    @Select("""
            SELECT p.id, p.code, p.pack_date, sk.code AS sku_code, sk.name AS sku_name,
              p.qty, p.unit
            FROM packing p
            LEFT JOIN sku sk ON p.sku_id = sk.id
            WHERE p.batch_id = #{batchId}
            ORDER BY p.pack_date ASC
            """)
    List<PackingNode> findPackings(@Param("batchId") Long batchId);

    @Select("""
            <script>
            SELECT DISTINCT so.id, so.code, so.order_date, c.name AS customer_name, so.status
            FROM sales_order so
            JOIN sales_order_item soi ON so.id = soi.order_id
            JOIN packing p ON soi.sku_id = p.sku_id
            LEFT JOIN customer c ON so.customer_id = c.id
            WHERE p.batch_id = #{batchId}
              AND so.status NOT IN ('cancelled', 'draft')
            ORDER BY so.order_date DESC
            </script>
            """)
    List<OrderNode> findOrders(@Param("batchId") Long batchId);
}
