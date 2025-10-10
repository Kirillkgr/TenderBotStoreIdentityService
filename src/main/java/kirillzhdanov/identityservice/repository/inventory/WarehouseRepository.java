package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findAllByMaster_Id(Long masterId);

    Optional<Warehouse> findByIdAndMaster_Id(Long id, Long masterId);

    boolean existsByMaster_IdAndNameIgnoreCase(Long masterId, String name);

    void deleteByIdAndMaster_Id(Long id, Long masterId);
}
