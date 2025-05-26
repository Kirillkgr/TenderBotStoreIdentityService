package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

	Optional<Token> findByToken(String token);

	@Query("SELECT t FROM Token t WHERE t.user.id = :userId AND t.revoked = false AND t.expired = false")
	List<Token> findAllValidTokensByUser(@Param("userId") Long userId);

	@Query("SELECT t FROM Token t WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.revoked = false AND t.expired = false")
	List<Token> findAllValidTokensByUserAndType(@Param("userId") Long userId, @Param("tokenType") Token.TokenType tokenType);
}
