package kirillzhdanov.identityservice.dto.group;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GroupTagTreeResponse {
    private Long id;
    private String name;
    private Long brandId;
    private Long parentId; // null or 0 -> root
    private Integer level;
    private List<GroupTagTreeResponse> children = new ArrayList<>();

    public GroupTagTreeResponse(Long id, String name, Long brandId, Long parentId, Integer level) {
        this.id = id;
        this.name = name;
        this.brandId = brandId;
        this.parentId = parentId;
        this.level = level;
    }

}
