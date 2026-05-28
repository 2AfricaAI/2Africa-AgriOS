package ai.toafrica.agrios.production.vo;

import ai.toafrica.agrios.packhouse.vo.PackingRow;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Batch detail - parent/children/packings full chain")
public class BatchDetailVO {
    @Schema(description = "This batch")
    private BatchVO batch;

    @Schema(description = "Parent batch (this batch was split from it); null = this is the root batch")
    private BatchVO parent;

    @Schema(description = "Children (batches split from this one); may be empty")
    private List<BatchVO> children;

    @Schema(description = "Packings linked to this batch (ordered by packed_at desc)")
    private List<PackingRow> packings;
}
