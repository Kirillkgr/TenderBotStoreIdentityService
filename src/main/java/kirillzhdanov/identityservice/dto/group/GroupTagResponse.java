package kirillzhdanov.identityservice.dto.group;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupTagResponse {
    private Long id;
    private String name;
    private Long brandId;
    private Long parentId;
    private int level;
    private List<GroupTagResponse> children;

    public GroupTagResponse(Long id, String name, Long brandId, Long parentId, int level) {
        this.id = id;
        this.name = name;
        this.brandId = brandId;
        this.parentId = parentId;
        this.level = level;
    }
}
