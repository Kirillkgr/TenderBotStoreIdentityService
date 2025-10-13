package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findAllByWarehouse_IdAndMaster_Id(Long warehouseId, Long masterId);

    Optional<Stock> findByMaster_IdAndWarehouse_IdAndIngredient_Id(Long masterId, Long warehouseId, Long ingredientId);

    List<Stock> findAllByIngredient_IdAndMaster_Id(Long ingredientId, Long masterId);
}
