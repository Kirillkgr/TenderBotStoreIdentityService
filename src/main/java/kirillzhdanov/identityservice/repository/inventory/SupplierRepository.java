package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByMaster_Id(Long masterId);

    Optional<Supplier> findByIdAndMaster_Id(Long id, Long masterId);

    boolean existsByMaster_IdAndNameIgnoreCase(Long masterId, String name);

    void deleteByIdAndMaster_Id(Long id, Long masterId);
}
