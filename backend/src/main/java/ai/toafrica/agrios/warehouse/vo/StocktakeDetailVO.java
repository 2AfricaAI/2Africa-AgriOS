package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.util.List;

@Data
public class StocktakeDetailVO {
    private StocktakeVO header;
    private List<StocktakeItemVO> items;
}
