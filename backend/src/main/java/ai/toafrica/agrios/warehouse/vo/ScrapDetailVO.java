package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.util.List;

@Data
public class ScrapDetailVO {
    private ScrapVO header;
    private List<ScrapItemVO> items;
}
