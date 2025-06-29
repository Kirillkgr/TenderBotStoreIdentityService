package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.Token;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

	Optional<Token> findByToken(String token);

	@Query("SELECT t FROM Token t WHERE t.user.id = :userId AND t.revoked = false")
	List<Token> findAllValidTokensByUser(@Param("userId") Long userId);

	List<Token> findAllByExpiryDateBefore(LocalDateTime date);
}
