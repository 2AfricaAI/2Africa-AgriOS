package ai.toafrica.agrios.common.exception;

import ai.toafrica.agrios.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("[业务异常] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::fieldErrorMsg)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] {}", msg);
        return R.fail(R.BUSINESS_ERROR, msg);
    }

    private String fieldErrorMsg(FieldError err) {
        return err.getField() + ": " + err.getDefaultMessage();
    }

    /** 唯一键冲突（例如批次号重复） */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicate(DuplicateKeyException e) {
        log.warn("[唯一键冲突] {}", e.getMessage());
        return R.fail(R.BUSINESS_ERROR, "Data already exists (unique constraint violation)");
    }

    /** 认证失败 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(R.UNAUTHORIZED, "Not authenticated or token expired"));
    }

    /** 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.fail(R.FORBIDDEN, "Insufficient permissions"));
    }

    /** 其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("[系统异常] {} {}", req.getMethod(), req.getRequestURI(), e);
        return R.fail(R.FAIL, "System busy, please try again later");
    }
}
