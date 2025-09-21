package kirillzhdanov.identityservice.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {

    @NotBlank(message = "Название обязательно")
    private String name;

    private String description;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false, message = "Акционная цена должна быть больше 0")
    private BigDecimal promoPrice;

    private Boolean visible;
}
