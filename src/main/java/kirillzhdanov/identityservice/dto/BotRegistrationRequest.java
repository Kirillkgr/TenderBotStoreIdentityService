package kirillzhdanov.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotRegistrationRequest {

	@NotBlank(message = "Bot token is required")
	private String botToken;

	private Long brandId;
}
