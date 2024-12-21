package bill.chat.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final String SECURITY_SCHEME_NAME = "authorization";

    @Bean
    public OpenAPI BillChatAPI() {

//        Info info = new Info()
//                .title("Bill-Chat Server API")
//                .description("Bill-Chat Server API 명세서")
//                .version("1.0.0");
//
//        String jwtSchemeName = "JWT TOKEN";
//        // API 요청헤더에 인증정보 포함
//        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
//        // SecuritySchemes 등록
//        Components components = new Components()
//                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
//                        .name(jwtSchemeName)
//                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
//                        .scheme("bearer")
//                        .bearerFormat("JWT"));
//
//        return new OpenAPI()
//                .addServersItem(new Server().url("/"))
//                .info(info)
//                .addSecurityItem(securityRequirement)
//                .components(components);
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .name(SECURITY_SCHEME_NAME)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .info(apiInfo());

    }

    private Info apiInfo() {
        return new Info()
                .title("Bill-Chat Server API")
                .description("Bill-Chat Server API 명세서")
                .version("1.0.0");
    }

    @Bean
    public GroupedOpenApi privateV1Api() {
        return GroupedOpenApi.builder()
                .group("privateV1")
                .pathsToMatch("/**")
                .build();
    }
}
