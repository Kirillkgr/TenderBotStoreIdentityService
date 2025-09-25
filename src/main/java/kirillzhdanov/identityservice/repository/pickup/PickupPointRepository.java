package kirillzhdanov.identityservice.repository.pickup;

import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupPointRepository extends JpaRepository<PickupPoint, Long> {
    List<PickupPoint> findByBrand_IdAndActiveTrue(Long brandId);
}
