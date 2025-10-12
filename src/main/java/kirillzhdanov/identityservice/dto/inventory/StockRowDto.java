package kirillzhdanov.identityservice.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockRowDto {
    private Long ingredientId;
    private String name;
    private Long unitId;
    private String unitName;
    private BigDecimal packageSize;
    private BigDecimal quantity;
    private String expiryDate;       // ISO yyyy-MM-dd or null
    private String lastSupplyDate;   // ISO yyyy-MM-dd or null
    private String lastUseDate;      // ISO yyyy-MM-dd or null
    private String supplierName;     // optional
    private String categoryName;     // optional
}
