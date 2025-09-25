package kirillzhdanov.identityservice.repository.userbrand;

import kirillzhdanov.identityservice.model.userbrand.UserBrandMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserBrandMembershipRepository extends JpaRepository<UserBrandMembership, Long> {
    Optional<UserBrandMembership> findByUser_IdAndBrand_Id(Long userId, Long brandId);

    List<UserBrandMembership> findByUser_Id(Long userId);

    @Query("select m.brand.id from UserBrandMembership m where m.user.id = :userId and m.brand is not null")
    List<Long> findBrandIdsByUserId(@Param("userId") Long userId);

    List<UserBrandMembership> findByBrand_Id(Long brandId);

    @Query("select m.user.id from UserBrandMembership m where m.brand.id = :brandId and m.user is not null")
    List<Long> findUserIdsByBrandId(@Param("brandId") Long brandId);
}
