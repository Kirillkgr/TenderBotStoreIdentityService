package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.SupplyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface SupplyItemRepository extends JpaRepository<SupplyItem, Long> {

    @Query("select min(si.expiresAt) from SupplyItem si " +
            "where si.ingredient.id = :ingredientId " +
            "and si.supply.warehouse.id = :warehouseId " +
            "and si.supply.master.id = :masterId")
    Optional<LocalDate> findMinExpiry(@Param("ingredientId") Long ingredientId,
                                      @Param("warehouseId") Long warehouseId,
                                      @Param("masterId") Long masterId);

    @Query("select max(s.date) from SupplyItem si join si.supply s " +
            "where si.ingredient.id = :ingredientId " +
            "and s.warehouse.id = :warehouseId " +
            "and s.master.id = :masterId")
    Optional<OffsetDateTime> findLastSupplyDate(@Param("ingredientId") Long ingredientId,
                                                @Param("warehouseId") Long warehouseId,
                                                @Param("masterId") Long masterId);
}
