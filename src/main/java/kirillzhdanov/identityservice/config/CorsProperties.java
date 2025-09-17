package kirillzhdanov.identityservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "address.service.front-service-cors")
public class CorsProperties {

	/**
	 * Список адресов CORS из YAML — автоматически заполняется Spring.
	 */
	private List<String> list;
}