package kirillzhdanov.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotRegistrationRequest {

	@NotBlank(message = "Bot token is required")
	private String botToken;

	private Long brandId;
}
