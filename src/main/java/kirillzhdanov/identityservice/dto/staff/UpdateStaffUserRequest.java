package kirillzhdanov.identityservice.dto.staff;

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
public class UpdateStaffUserRequest {
    private String lastName;
    private String firstName;
    private String patronymic;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String login; // username
    private String password;
    private Long departmentId; // nullable
    private Set<String> roles; // USER, ADMIN, OWNER
}
