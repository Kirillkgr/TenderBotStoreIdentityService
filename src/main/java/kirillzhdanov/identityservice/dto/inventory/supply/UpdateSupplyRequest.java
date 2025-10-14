package kirillzhdanov.identityservice.dto.inventory.supply;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class UpdateSupplyRequest {
    private Long warehouseId; // optional
    private Long supplierId; // optional
    private OffsetDateTime date; // optional
    private String notes; // optional

    @Valid
    private List<Item> items; // optional: replace all items

    @Data
    public static class Item {
        private Long ingredientId; // optional, but required if items provided
        @Min(0)
        private Double qty;       // required >0
        private LocalDate expiresAt; // optional
    }
}
