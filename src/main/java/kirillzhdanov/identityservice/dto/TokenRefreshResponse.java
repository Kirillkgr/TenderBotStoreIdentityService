package kirillzhdanov.identityservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponse {

	private String accessToken;

	private String refreshToken;

	private String tokenType = "Bearer";
}
