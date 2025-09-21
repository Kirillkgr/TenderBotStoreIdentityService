package kirillzhdanov.identityservice.dto.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductArchiveResponse {
    private Long id; // archive id
    private Long originalProductId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal promoPrice;
    private Long brandId;
    private Long groupTagId;
    private String groupPath;
    private boolean visible;
    private LocalDateTime archivedAt;
}
