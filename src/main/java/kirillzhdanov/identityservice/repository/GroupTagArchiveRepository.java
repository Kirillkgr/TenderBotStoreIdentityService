package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.tags.GroupTagArchive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GroupTagArchiveRepository extends JpaRepository<GroupTagArchive, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GroupTagArchive a where a.archivedAt < :threshold")
    int deleteAllOlderThan(@Param("threshold") LocalDateTime threshold);

    long countByArchivedAtBefore(LocalDateTime threshold);

    List<GroupTagArchive> findByBrandId(Long brandId);

    Page<GroupTagArchive> findByBrandId(Long brandId, Pageable pageable);

    Optional<GroupTagArchive> findByBrandIdAndPath(Long brandId, String path);
}
