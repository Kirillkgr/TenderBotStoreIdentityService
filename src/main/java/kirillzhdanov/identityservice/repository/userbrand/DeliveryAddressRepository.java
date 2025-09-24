package kirillzhdanov.identityservice.repository.userbrand;

import kirillzhdanov.identityservice.model.userbrand.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    List<DeliveryAddress> findByMembership_IdAndDeletedFalse(Long membershipId);
}
