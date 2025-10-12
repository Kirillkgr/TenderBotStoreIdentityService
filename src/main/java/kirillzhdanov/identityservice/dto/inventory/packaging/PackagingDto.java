package kirillzhdanov.identityservice.dto.inventory.packaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagingDto {
    private Long id;
    private String name;
    private Long unitId;
    private Double size;
}
