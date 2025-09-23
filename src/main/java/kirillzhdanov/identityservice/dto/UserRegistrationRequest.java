package kirillzhdanov.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import kirillzhdanov.identityservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

	@NotBlank(message = "Имя пользователя не может быть пустым")
	private String username;

	@NotBlank(message = "Пароль не может быть пустым")
	private String password;

	private String firstName;
	private String lastName;
	private String patronymic;
	private LocalDate dateOfBirth;

	// contact data
	private String email;
	private String phone;

	private String telegramBotToken;

	private Set<Role.RoleName> roleNames;

	private Set<Long> brandIds;
}
