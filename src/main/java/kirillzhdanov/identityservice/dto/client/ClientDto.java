package kirillzhdanov.identityservice.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private LocalDateTime lastOrderAt;
    private Long lastOrderBrandId;
    private String lastOrderBrand;
}
