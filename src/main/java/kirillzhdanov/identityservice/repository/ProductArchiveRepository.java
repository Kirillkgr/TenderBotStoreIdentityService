package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.product.ProductArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ProductArchiveRepository extends JpaRepository<ProductArchive, Long> {
    List<ProductArchive> findByBrandId(Long brandId);
    long deleteByArchivedAtBefore(LocalDateTime threshold);
}
