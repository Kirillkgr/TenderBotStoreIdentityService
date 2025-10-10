package kirillzhdanov.identityservice.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String address;
}
