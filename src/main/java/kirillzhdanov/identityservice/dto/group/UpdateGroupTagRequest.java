package kirillzhdanov.identityservice.dto.group;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateGroupTagRequest {
    private String name;
    private Long parentId; // 0 or null -> root
    private Long brandId;  // optional, to move subtree to another brand

}
