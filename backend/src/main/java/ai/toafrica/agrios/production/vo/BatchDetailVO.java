package ai.toafrica.agrios.production.vo;

import ai.toafrica.agrios.packhouse.vo.PackingRow;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批次详情 - 含父/子/包装单的完整溯源")
public class BatchDetailVO {
    @Schema(description = "本批次")
    private BatchVO batch;

    @Schema(description = "父批次(本批次是从它拆出来的);为 null 则本批次是根")
    private BatchVO parent;

    @Schema(description = "子批次(本批次拆出来的);可能为空")
    private List<BatchVO> children;

    @Schema(description = "本批次相关的包装单 (按 packed_at desc)")
    private List<PackingRow> packings;
}
