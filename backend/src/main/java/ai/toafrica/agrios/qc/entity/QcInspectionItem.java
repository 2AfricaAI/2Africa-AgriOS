package ai.toafrica.agrios.qc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("qc_inspection_item")
public class QcInspectionItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private String checkPoint;
    private String expectedValue;
    private String actualValue;
    private String result;
    private String remark;
}
