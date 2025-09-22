package kirillzhdanov.identityservice.dto.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTagArchiveResponse {
    private Long id; // archive id
    private Long originalGroupTagId;
    private Long brandId;
    private Long parentId;
    private String name;
    private String path;
    private int level;
    private LocalDateTime archivedAt;
}
