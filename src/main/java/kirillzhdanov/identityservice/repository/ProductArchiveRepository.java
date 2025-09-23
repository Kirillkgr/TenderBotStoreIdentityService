package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.product.ProductArchive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductArchiveRepository extends JpaRepository<ProductArchive, Long> {
    List<ProductArchive> findByBrandId(Long brandId);

    Page<ProductArchive> findByBrandId(Long brandId, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("delete from ProductArchive p where p.archivedAt < :threshold")
    int deleteByArchivedAtBefore(@Param("threshold") LocalDateTime threshold);
}
