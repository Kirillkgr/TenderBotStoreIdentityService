package kirillzhdanov.identityservice.dto.inventory.supply;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

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
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long ingredientId;
        @NotNull
        @Min(0)
        private Double qty;
        private java.time.LocalDate expiresAt; // optional
    }
}
