package ai.toafrica.agrios.common.exception;

import ai.toafrica.agrios.common.R;
import lombok.Getter;

/**
 * 业务异常 - 由业务规则失败抛出
 * 例：库存不足、状态机非法跳转、唯一约束冲突
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String msg) {
        super(msg);
        this.code = R.BUSINESS_ERROR;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    /** 库存不足 */
    public static BusinessException insufficientInventory(Long skuId) {
        return new BusinessException("SKU#" + skuId + " 库存不足，无法锁定");
    }

    /** 状态机非法跳转 */
    public static BusinessException illegalStateTransition(String from, String to) {
        return new BusinessException("非法状态流转: " + from + " → " + to);
    }

    /** 资源不存在 */
    public static BusinessException notFound(String resource, Object id) {
        return new BusinessException(R.NOT_FOUND, resource + " 不存在: " + id);
    }
}
