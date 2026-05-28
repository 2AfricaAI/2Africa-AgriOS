package ai.toafrica.agrios.system.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.system.dto.LoginDTO;
import ai.toafrica.agrios.system.dto.LoginVO;
import ai.toafrica.agrios.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "01 · Auth", description = "Login / logout / current user")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Username + password login")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest req) {
        return R.ok(authService.login(dto, getClientIp(req)));
    }

    /** 提取客户端真实 IP - 兼容反向代理 (Nginx / 网关) */
    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = req.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return req.getRemoteAddr();
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return R.ok();
    }

    @Operation(summary = "Current user info")
    @GetMapping("/me")
    public R<Object> me() {
        return R.ok(SecurityUtil.current());
    }
}
