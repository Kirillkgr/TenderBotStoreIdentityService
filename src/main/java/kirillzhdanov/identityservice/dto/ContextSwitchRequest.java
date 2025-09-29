package kirillzhdanov.identityservice.dto;

import lombok.Data;

@Data
public class ContextSwitchRequest {
    private Long membershipId; // required
    private Long brandId;      // optional override
    private Long locationId;   // optional override
}
