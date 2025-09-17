package kirillzhdanov.identityservice.dto.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicProductResponse {
    private Long id;
    private String name;
    private String description; // добавлено описание для модалки
    private BigDecimal price;
    private BigDecimal promoPrice;
}
