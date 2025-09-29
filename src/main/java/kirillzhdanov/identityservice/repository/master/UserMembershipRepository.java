package kirillzhdanov.identityservice.repository.master;

import kirillzhdanov.identityservice.model.master.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {
    List<UserMembership> findByUserId(Long userId);

    List<UserMembership> findByMasterId(Long masterId);

    Optional<UserMembership> findByUserIdAndBrandId(Long userId, Long brandId);

    Optional<UserMembership> findByUserIdAndMasterId(Long userId, Long masterId);
}
