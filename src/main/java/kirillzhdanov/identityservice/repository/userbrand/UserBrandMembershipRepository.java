package kirillzhdanov.identityservice.repository.userbrand;

import kirillzhdanov.identityservice.model.userbrand.UserBrandMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBrandMembershipRepository extends JpaRepository<UserBrandMembership, Long> {
    Optional<UserBrandMembership> findByUser_IdAndBrand_Id(Long userId, Long brandId);
}
