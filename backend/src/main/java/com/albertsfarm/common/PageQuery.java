package com.albertsfarm.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用分页查询参数
 */
@Data
@Schema(description = "通用分页查询参数")
public class PageQuery {
    @Schema(description = "页码（从 1 开始）", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "排序字段，如 createdAt")
    private String sort;

    @Schema(description = "排序方向 asc/desc", defaultValue = "desc")
    private String order = "desc";

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getSize() {
        if (size == null || size < 1) return 20;
        if (size > 200) return 200;
        return size;
    }
}
