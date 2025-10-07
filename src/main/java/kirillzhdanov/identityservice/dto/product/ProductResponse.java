package kirillzhdanov.identityservice.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal promoPrice;
    private Long brandId;
    private Long groupTagId; // null означает корень
    private boolean visible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
