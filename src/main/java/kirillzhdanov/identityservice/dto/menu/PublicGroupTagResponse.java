package kirillzhdanov.identityservice.dto.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicGroupTagResponse {
    private Long id;
    private String name;
    private Long parentId; // null для корня
    private int level;     // 0 для корня
}
