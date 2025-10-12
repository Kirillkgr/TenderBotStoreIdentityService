package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findAllByWarehouse_IdAndMaster_Id(Long warehouseId, Long masterId);

    java.util.Optional<Stock> findByMaster_IdAndWarehouse_IdAndIngredient_Id(Long masterId, Long warehouseId, Long ingredientId);
}
