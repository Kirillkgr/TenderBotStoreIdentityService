package kirillzhdanov.identityservice.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientWithStockDto {
    private Long id;
    private String name;
    private Long unitId;
    private String unitName;
    private BigDecimal packageSize;
    private String notes;

    private Long warehouseId;   // requested warehouse
    private BigDecimal quantity; // qty on that warehouse (0 if none)
    private String warehouseName; // convenience
    private String lastSupplyDate; // ISO-8601 string or null
}
