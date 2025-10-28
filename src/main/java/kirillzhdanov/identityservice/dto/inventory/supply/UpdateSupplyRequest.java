package kirillzhdanov.identityservice.dto.inventory.supply;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
public class UpdateSupplyRequest {
    private Long warehouseId; // optional
    private Long supplierId; // optional
    private OffsetDateTime date; // optional
    private String notes; // optional

    @Valid
    private List<@Valid Item> items; // optional: replace all items

    @Data
    public static class Item {
        private Long ingredientId; // optional, but required if items provided
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal qty;       // required >0
        @DecimalMin(value = "0")
        private BigDecimal unitCost;  // optional, but if provided must be >= 0
        private LocalDate expiresAt; // optional
    }
}
