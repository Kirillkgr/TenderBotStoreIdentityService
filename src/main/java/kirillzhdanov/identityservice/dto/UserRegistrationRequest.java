package kirillzhdanov.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import kirillzhdanov.identityservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

	private String telegramBotToken;

	private Set<Role.RoleName> roleNames;

	private Set<Long> brandIds;
}
