package kirillzhdanov.identityservice.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private Long id;
    private Long clientId;
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private Long brandId;
    private String brandName;
    private String status;
    private String deliveryMode;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private String comment;
    private List<OrderItemDto> items;
    // Client review summary
    private Integer rating; // 1..5
    private String reviewComment;
}
