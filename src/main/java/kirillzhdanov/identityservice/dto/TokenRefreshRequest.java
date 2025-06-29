package kirillzhdanov.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshRequest {

	@NotBlank(message = "Токен обновления не может быть пустым")
	private String refreshToken;
}
