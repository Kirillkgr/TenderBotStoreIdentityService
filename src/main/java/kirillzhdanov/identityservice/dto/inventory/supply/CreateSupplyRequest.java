package kirillzhdanov.identityservice.dto.inventory.supply;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
public class CreateSupplyRequest {
    @NotNull
    private Long warehouseId;
    private Long supplierId; // optional
    @NotNull
    private OffsetDateTime date;
    private String notes;

    @Valid
    @NotNull
    @NotEmpty
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long ingredientId;
        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal qty;
        @DecimalMin(value = "0")
        private BigDecimal unitCost; // optional, but if provided must be >= 0
        private java.time.LocalDate expiresAt; // optional
    }
}
