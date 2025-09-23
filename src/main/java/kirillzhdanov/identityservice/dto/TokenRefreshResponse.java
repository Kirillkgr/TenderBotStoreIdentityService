package kirillzhdanov.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponse {

	private String accessToken;

	private String refreshToken;

	@Builder.Default
	private String tokenType = "Bearer";
}
