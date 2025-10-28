package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long>, JpaSpecificationExecutor<StockBatch> {
    Optional<StockBatch> findByIdAndMaster_Id(Long id, Long masterId);
    List<StockBatch> findAllByMaster_Id(Long masterId);
    List<StockBatch> findAllByMaster_IdAndIngredient_Id(Long masterId, Long ingredientId);
    List<StockBatch> findAllByMaster_IdAndWarehouse_Id(Long masterId, Long warehouseId);
}
