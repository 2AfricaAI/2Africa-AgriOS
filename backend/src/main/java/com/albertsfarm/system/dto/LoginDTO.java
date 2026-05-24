package com.albertsfarm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginDTO {
    @NotBlank
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank
    @Schema(description = "密码", example = "Admin@123456")
    private String password;
}
