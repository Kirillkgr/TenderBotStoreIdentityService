package kirillzhdanov.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshRequest {

	@NotBlank(message = "Токен обновления не может быть пустым")
	private String refreshToken;
}
