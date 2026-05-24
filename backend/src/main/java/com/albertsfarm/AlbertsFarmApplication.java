package com.albertsfarm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Albert's Farm Agri OS - Backend Main Entry
 *
 * @author Albert's Farm Team
 * @since 1.0.0
 */
// 项目用 JWT 自带鉴权,不需要 Spring Security 默认的内存 UserDetailsService,
// 排除掉对应的 AutoConfiguration 以消除启动时的 "Using generated security password" 警告
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableAsync
@EnableScheduling
@MapperScan("com.albertsfarm.**.mapper")
public class AlbertsFarmApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlbertsFarmApplication.class, args);
        System.out.println("""
                ============================================================
                  Albert's Farm Agri OS - Backend Started
                  Swagger:  http://localhost:8080/api/swagger-ui.html
                  API Docs: http://localhost:8080/api/v3/api-docs
                ============================================================""");
    }
}
