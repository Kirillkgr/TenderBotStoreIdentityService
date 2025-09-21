package kirillzhdanov.identityservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityServiceOpenAPI() {
        return new OpenAPI()
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
}
