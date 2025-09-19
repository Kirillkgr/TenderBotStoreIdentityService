package kirillzhdanov.identityservice.dto.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentCreateRequest {
    @NotBlank
    private String name;
    private String description;
}
