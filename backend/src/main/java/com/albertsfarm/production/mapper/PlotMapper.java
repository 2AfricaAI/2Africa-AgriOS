package com.albertsfarm.production.mapper;

import com.albertsfarm.production.entity.Plot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlotMapper extends BaseMapper<Plot> {

    /**
     * 自定义分页（含负责人名称等关联字段），见 PlotMapper.xml
     */
    IPage<PlotVO> pageWithOwner(Page<?> page, @Param("q") PlotQueryVO q);

    /**
     * Plot 详情统计（近 30 天农事次数、采收量等），见 PlotMapper.xml
     */
    PlotStatVO statsLast30Days(@Param("plotId") Long plotId);

    // ===== 内嵌结果对象 =====
    @lombok.Data
    class PlotVO {
        private Long id;
        private String code;
        private String name;
        private java.math.BigDecimal areaMu;
        private Long ownerId;
        private String ownerName;
        private String status;
        private String currentCropName;
        private java.time.LocalDateTime updatedAt;
    }

    @lombok.Data
    class PlotQueryVO {
        private String code;
        private String name;
        private Long ownerId;
        private String status;
    }

    @lombok.Data
    class PlotStatVO {
        private Integer activityCount;
        private java.math.BigDecimal totalHarvestKg;
        private Integer batchCount;
    }
}
