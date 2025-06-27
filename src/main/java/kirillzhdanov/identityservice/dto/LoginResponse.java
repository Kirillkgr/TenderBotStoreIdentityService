package kirillzhdanov.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @deprecated Use {@link UserResponse} instead which includes user information along with tokens
 */
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

	private String accessToken;

	private String refreshToken;
}
