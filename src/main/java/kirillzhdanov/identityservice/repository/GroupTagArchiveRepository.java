package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.tags.GroupTagArchive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GroupTagArchiveRepository extends JpaRepository<GroupTagArchive, Long> {
    long deleteByArchivedAtBefore(LocalDateTime threshold);

    List<GroupTagArchive> findByBrandId(Long brandId);

    Page<GroupTagArchive> findByBrandId(Long brandId, Pageable pageable);
}
