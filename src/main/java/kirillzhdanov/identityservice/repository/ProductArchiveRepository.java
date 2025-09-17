package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.product.ProductArchive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductArchiveRepository extends JpaRepository<ProductArchive, Long> {
}
