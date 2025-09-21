package kirillzhdanov.identityservice.model.tags;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_tag_archive")
@Getter
@Setter
@NoArgsConstructor
public class GroupTagArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long originalGroupTagId;

    @Column(nullable = false)
    private Long brandId;

    private Long parentId; // parent at time of archiving, may be null

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path; // path at the time of archiving (e.g. "/1/4/")

    @Column(nullable = false)
    private int level; // level at the time of archiving

    @Column(nullable = false)
    private LocalDateTime archivedAt;
}
