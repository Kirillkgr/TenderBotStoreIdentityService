package kirillzhdanov.identityservice.dto.order;

import jakarta.validation.constraints.NotNull;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    @NotNull
    private OrderStatus newStatus;
    private String comment; // optional operator comment
}
