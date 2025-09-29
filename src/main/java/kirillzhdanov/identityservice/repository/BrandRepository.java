package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

	boolean existsByName(String name);

	// Context-scoped helpers
	java.util.List<Brand> findByMaster_Id(Long masterId);

	java.util.Optional<Brand> findByIdAndMaster_Id(Long id, Long masterId);

	boolean existsByNameAndMaster_Id(String name, Long masterId);
}
