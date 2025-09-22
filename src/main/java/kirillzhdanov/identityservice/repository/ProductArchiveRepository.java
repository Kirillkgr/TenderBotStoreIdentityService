package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.product.ProductArchive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductArchiveRepository extends JpaRepository<ProductArchive, Long> {
    List<ProductArchive> findByBrandId(Long brandId);

    Page<ProductArchive> findByBrandId(Long brandId, Pageable pageable);
    long deleteByArchivedAtBefore(LocalDateTime threshold);
}
