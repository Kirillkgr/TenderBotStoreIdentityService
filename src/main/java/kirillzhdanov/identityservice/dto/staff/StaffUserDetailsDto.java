package kirillzhdanov.identityservice.dto.staff;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffUserDetailsDto {
    private Long id;
    private String login;
    private String lastName;
    private String firstName;
    private String patronymic;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private Set<String> roles; // codes
    private Long departmentId;
    private String departmentName;
    private Long masterId;
    private LocalDateTime createdAt;
}
