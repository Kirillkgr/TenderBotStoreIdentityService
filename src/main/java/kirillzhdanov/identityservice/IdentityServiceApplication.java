package kirillzhdanov.identityservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(
				title = "Identity Service API",
				version = "1.0.0",
				description = "API для управления пользователями и Telegram-ботами"
		)
)
public class IdentityServiceApplication {

	public static void main(String[] args){

		SpringApplication.run(IdentityServiceApplication.class, args);
	}

}