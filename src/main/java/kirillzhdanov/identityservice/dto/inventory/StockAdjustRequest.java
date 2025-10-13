package kirillzhdanov.identityservice.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustRequest {
    @NotNull
    private Long ingredientId;
    @NotNull
    private Long warehouseId;
    @NotNull
    @Min(0)
    private Double qty; // абсолютная величина изменения (положительная)
}
