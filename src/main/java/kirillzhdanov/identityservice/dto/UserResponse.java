package kirillzhdanov.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

	private Long id;

	private String username;

	private Set<String> roles;

	private Set<BrandDto> brands;

	private String accessToken;

	private String refreshToken;
}
