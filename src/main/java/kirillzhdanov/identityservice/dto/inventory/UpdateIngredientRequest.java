package kirillzhdanov.identityservice.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateIngredientRequest {
    @NotBlank(message = "Ingredient name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @NotNull(message = "unitId is required")
    private Long unitId;

    @Min(value = 0, message = "packageSize must be >= 0")
    private BigDecimal packageSize;

    @Size(max = 1024, message = "Notes must be at most 1024 characters")
    private String notes;
}
