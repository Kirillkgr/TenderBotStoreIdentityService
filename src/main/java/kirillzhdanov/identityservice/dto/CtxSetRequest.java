package kirillzhdanov.identityservice.dto;

import lombok.Data;

@Data
public class CtxSetRequest {
    private Long masterId;
    private Long brandId;
    private Long pickupPointId;
}
