package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Supply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplyRepository extends JpaRepository<Supply, Long>, JpaSpecificationExecutor<Supply> {
    Optional<Supply> findByIdAndMaster_Id(Long id, Long masterId);

    @Query("select s from Supply s " +
            "left join fetch s.warehouse w " +
            "left join fetch s.items si " +
            "left join fetch si.ingredient ing " +
            "where s.id = :id and s.master.id = :masterId")
    Optional<Supply> findDetailedByIdAndMaster(@Param("id") Long id, @Param("masterId") Long masterId);
}
