package kirillzhdanov.identityservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

	@Bean
	public OpenAPI openAPI(){

		return new OpenAPI()
				.info(new Info()
						.title("Identity Service API")
						.description("API для управления пользователями и Telegram-ботами")
						.version("1.0.0")
						.contact(new Contact()
								.name("TenderBot Team")
								.email("support@tenderbot.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components()
						.addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
								.name(SECURITY_SCHEME_NAME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Введите JWT токен в формате: Bearer {token}")));
	}

	@Bean
	public GroupedOpenApi authApi(){

		return GroupedOpenApi.builder()
				.group("auth-api")
				.pathsToMatch("/auth")
				.displayName("Аутентификация")
				.build();
	}

	@Bean
	public GroupedOpenApi botApi(){

		return GroupedOpenApi.builder()
				.group("bot-api")
				.pathsToMatch("/bot")
				.displayName("Telegram-боты")
				.build();
	}

	@Bean
	public GroupedOpenApi allApi(){

		return GroupedOpenApi.builder()
				.group("all-api")
				.pathsToMatch("/")
				.displayName("Все API")
				.build();
	}
}
