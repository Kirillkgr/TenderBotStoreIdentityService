package kirillzhdanov.identityservice.dto.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicBrandMinResponse {
    private Long id;
    private String domain;
}
