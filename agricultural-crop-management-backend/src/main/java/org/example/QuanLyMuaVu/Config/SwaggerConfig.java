package org.example.QuanLyMuaVu.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Test API Quản Lý Mùa Vụ")
                        .version("1.0.0")
                        .description("""
                            ### Cách test API bằng Swagger:
                            - Đi tới dòng POST /auth/login thực hiện đăng nhập
                            - Đăng nhập trả về Bearer token
                            - Copy token từ response login
                            - Nhấn vào biểu tượng ổ khóa đang mở
                            - Dán token đã copy vào textbox Value
                            - Nhấn nút Authorize
                            - Sử dụng nút "Try it out" trong Swagger UI
                            - Test các endpoints được bảo vệ
                            """))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("🖥️ Local Development Server"),
                        new Server()
                                .url("https://api.quanlymuavu.com")
                                .description("🌐 Production Server")
                ))

                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                ));
    }
}
