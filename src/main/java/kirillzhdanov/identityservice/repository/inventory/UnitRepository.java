package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findAllByMaster_Id(Long masterId);

    Optional<Unit> findByIdAndMaster_Id(Long id, Long masterId);

    boolean existsByMaster_IdAndNameIgnoreCase(Long masterId, String name);

    void deleteByIdAndMaster_Id(Long id, Long masterId);
}
