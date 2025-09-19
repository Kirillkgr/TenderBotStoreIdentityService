package kirillzhdanov.identityservice.dto.staff;

import jakarta.validation.constraints.*;
import lombok.*;

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
