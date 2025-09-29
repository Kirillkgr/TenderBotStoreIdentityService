package kirillzhdanov.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContextSwitchResponse {
    private String accessToken;
}
