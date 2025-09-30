package kirillzhdanov.identityservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MembershipDto {
    private Long membershipId;
    private Long masterId;
    private String masterName;
    private Long brandId;
    private String brandName;
    private Long locationId;
    private String locationName;
    private String role;
    private String status;
}
