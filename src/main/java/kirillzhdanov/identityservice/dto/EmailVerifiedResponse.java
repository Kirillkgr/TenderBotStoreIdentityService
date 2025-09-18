package kirillzhdanov.identityservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerifiedResponse {
    private boolean verified;
}
