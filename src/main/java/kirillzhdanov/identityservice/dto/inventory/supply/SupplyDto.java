package kirillzhdanov.identityservice.dto.inventory.supply;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
public class SupplyDto {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long supplierId;
    private OffsetDateTime date;
    private String notes;
    private String status;
    private BigDecimal totalCost; // nullable
    private List<Item> items;

    @Data
    public static class Item {
        private Long ingredientId;
        private String ingredientName;
        private Double qty;
        private BigDecimal unitCost; // nullable
        private LocalDate expiresAt;
    }
}
