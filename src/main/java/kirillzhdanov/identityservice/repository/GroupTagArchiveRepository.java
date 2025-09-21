package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.tags.GroupTagArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface GroupTagArchiveRepository extends JpaRepository<GroupTagArchive, Long> {
    long deleteByArchivedAtBefore(LocalDateTime threshold);
}
