package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Packaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackagingRepository extends JpaRepository<Packaging, Long> {
    List<Packaging> findAllByMaster_Id(Long masterId);

    Optional<Packaging> findByIdAndMaster_Id(Long id, Long masterId);

    boolean existsByMaster_IdAndNameIgnoreCase(Long masterId, String name);
}
