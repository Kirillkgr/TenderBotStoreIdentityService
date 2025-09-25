package kirillzhdanov.identityservice.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long masterId;
    private String masterName;
}
