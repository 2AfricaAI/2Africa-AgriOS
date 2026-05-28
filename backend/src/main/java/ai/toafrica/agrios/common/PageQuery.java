package ai.toafrica.agrios.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用分页查询参数
 */
@Data
@Schema(description = "Generic pagination query params")
public class PageQuery {
    @Schema(description = "Page number (1-based)", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "Page size", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "Sort field, e.g. createdAt")
    private String sort;

    @Schema(description = "Sort direction asc/desc", defaultValue = "desc")
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
