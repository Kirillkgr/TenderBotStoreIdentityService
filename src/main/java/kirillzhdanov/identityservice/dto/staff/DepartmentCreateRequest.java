package kirillzhdanov.identityservice.dto.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentCreateRequest {
    @NotBlank
    private String name;
    private String description;
}
