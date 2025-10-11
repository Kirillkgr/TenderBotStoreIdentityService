package kirillzhdanov.identityservice.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWarehouseRequest {
    @NotBlank(message = "Warehouse name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;
}
