package ai.toafrica.agrios.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一响应信封
 * <pre>
 * {
 *   "code": 200,
 *   "msg":  "OK",
 *   "data": {...},
 *   "traceId": "abc-123",
 *   "timestamp": "2026-05-23T08:00:00"
 * }
 * </pre>
 */
@Data
@Accessors(chain = true)
public class R<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final int OK = 200;
    public static final int FAIL = 500;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int BUSINESS_ERROR = 400;

    private int code;
    private String msg;
    private T data;
    private String traceId;
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<T>()
                .setCode(OK)
                .setMsg("OK")
                .setData(data)
                .setTraceId(UUID.randomUUID().toString());
    }

    public static <T> R<T> fail(String msg) {
        return fail(FAIL, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<T>().setCode(code).setMsg(msg)
                .setTraceId(UUID.randomUUID().toString());
    }
}
