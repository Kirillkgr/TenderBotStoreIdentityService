package kirillzhdanov.identityservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityServiceOpenAPI() {
        // Security scheme: Bearer JWT
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearer-jwt", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .addServersItem(new Server().url("http://localhost:8080").description("Dev"))
                .addServersItem(new Server().url("https://kirillkgr.ru").description("Prod"))
                .info(new Info()
                        .title("TenderBotStore Identity Service API")
                        .description("Auth, users, brands, group tags, products and archives")
                        .version("v1")
                        .contact(new Contact()
                                .name("Zhdanov Kirill")
                                .email("RillGd@gmail.com")
                                .url("https://github.com/Kirillkgr"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                )
                .externalDocs(new ExternalDocumentation()
                        .description("Repository")
                        .url("https://github.com/Kirillkgr/TenderBotStoreIdentityService"));
    }

    // Public group: only public endpoints, without security requirements
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch(
                        "/public/**",
                        "/menu/**",
                        "/notifications/longpoll",
                        "/auth/v1/login",
                        "/auth/v1/register",
                        "/auth/v1/refresh",
                        "/auth/v1/checkUsername",
                        "/status"
                )
                .addOpenApiCustomizer(removeSecurityCustomizer())
                .build();
    }

    // Secure group: everything else (excluding swagger and public endpoints), inherits global JWT security
    @Bean
    public GroupedOpenApi secureApi() {
        return GroupedOpenApi.builder()
                .group("secure")
                .pathsToMatch("/**")
                .pathsToExclude(
                        // swagger & api-docs
                        "/auth/swagger",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/auth/api-docs/**",
                        // public endpoints
                        "/public/**",
                        "/menu/**",
                        "/notifications/longpoll",
                        "/auth/v1/login",
                        "/auth/v1/register",
                        "/auth/v1/refresh",
                        "/auth/v1/checkUsername",
                        "/status"
                )
                .build();
    }

    // Customizer to strip security from the public group
    @Bean
    public OpenApiCustomizer removeSecurityCustomizer() {
        return openApi -> {
            // Remove top-level security requirement
            openApi.setSecurity(null);
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem == null) return;
                pathItem.readOperations().forEach(op -> op.setSecurity(null));
            });
        };
    }
}

