package kirillzhdanov.identityservice.repository.inventory;

import kirillzhdanov.identityservice.model.inventory.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByMaster_Id(Long masterId);

    Optional<Ingredient> findByIdAndMaster_Id(Long id, Long masterId);

    boolean existsByMaster_IdAndNameIgnoreCase(Long masterId, String name);

    void deleteByIdAndMaster_Id(Long id, Long masterId);
}
