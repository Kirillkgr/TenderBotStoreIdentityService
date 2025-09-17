package kirillzhdanov.identityservice.dto.product;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateRequest {

    @NotBlank(message = "Название обязательно")
    private String name;

    private String description;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false, message = "Акционная цена должна быть больше 0")
    private BigDecimal promoPrice;

    @NotNull(message = "Бренд обязателен")
    private Long brandId;

    // 0 или null означает товар в корне бренда
    private Long groupTagId;

    // Флаг видимости
    private boolean visible = true;
}
