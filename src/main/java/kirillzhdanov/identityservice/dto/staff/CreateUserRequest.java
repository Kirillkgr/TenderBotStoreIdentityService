package kirillzhdanov.identityservice.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    @NotBlank
    private String lastName;
    @NotBlank
    private String firstName;
    private String patronymic;

    @NotNull
    private LocalDate birthDate;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    private String login; // username

    @NotBlank
    private String password;

    private Long departmentId; // nullable

    @NotNull
    private Long masterId;

    @NotEmpty
    private Set<String> roles; // USER, ADMIN, OWNER
}
