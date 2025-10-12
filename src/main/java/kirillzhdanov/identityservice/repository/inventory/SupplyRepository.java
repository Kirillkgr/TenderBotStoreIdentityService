package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Supply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplyRepository extends JpaRepository<Supply, Long> {
    Optional<Supply> findByIdAndMaster_Id(Long id, Long masterId);
}
