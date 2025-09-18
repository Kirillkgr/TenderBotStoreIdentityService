package kirillzhdanov.identityservice.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String patronymic;
    private LocalDate dateOfBirth;
    private String email;    // может совпадать с текущим либо быть уже подтверждённым
    private String phone;
}
