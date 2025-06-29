package kirillzhdanov.identityservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

	private Long id;

	private String username;

	private String firstName;
	private String lastName;
	private String patronymic;
	private LocalDate dateOfBirth;

	private Set<String> roles;

	private Set<BrandDto> brands;

	private String accessToken;

	private String refreshToken;
}
