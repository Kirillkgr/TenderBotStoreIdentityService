package kirillzhdanov.identityservice.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListItemDto {
    private Long id;
    private String login;
    private String lastName;
    private String firstName;
    private String patronymic;
    private Set<String> roles; // codes
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
}
