package kirillzhdanov.identityservice.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGroupTagRequest {
    @NotBlank(message = "Group tag name is required")
    private String name;
    
    @NotNull(message = "Brand ID is required")
    private Long brandId;
    
    private Long parentId; // null or 0 for root
}
