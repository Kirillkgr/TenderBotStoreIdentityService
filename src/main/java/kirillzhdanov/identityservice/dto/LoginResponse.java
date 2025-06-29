package kirillzhdanov.identityservice.dto;

import lombok.*;

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
