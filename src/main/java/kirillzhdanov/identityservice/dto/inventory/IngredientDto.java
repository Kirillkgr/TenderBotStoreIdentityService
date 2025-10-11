package kirillzhdanov.identityservice.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientDto {
    private Long id;
    private String name;
    private Long unitId;
    private String unitName;
    private BigDecimal packageSize;
    private String notes;
}
