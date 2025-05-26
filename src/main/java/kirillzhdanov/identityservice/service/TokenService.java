package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.TokenRepository;
import kirillzhdanov.identityservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {

	private final TokenRepository tokenRepository;

	private final JwtUtils jwtUtils;

	@Transactional
	public void saveToken(String tokenString, Token.TokenType tokenType, User user){
		// Проверяем входные параметры
		if(tokenString == null || tokenType == null || user == null) {
			throw new IllegalArgumentException("Параметры токена не могут быть null");
		}

		// Проверяем, существует ли уже такой токен
		Optional<Token> existingToken = tokenRepository.findByToken(tokenString);
		if(existingToken.isPresent()) {
			// Если токен существует, но отозван или истек, обновляем его
			Token token = existingToken.get();
			if(token.isRevoked() || token.isExpired()) {
				token.setRevoked(false);
				token.setExpired(false);
				token.setExpiresAt(jwtUtils.extractExpirationAsLocalDateTime(tokenString));
				tokenRepository.save(token);
				return;
			}
			// Если токен уже существует и действителен, ничего не делаем
			return;
		}

		// Создаем новый токен
		Token token = Token.builder()
				.token(tokenString)
				.tokenType(tokenType)
				.revoked(false)
				.expired(false)
				.expiresAt(jwtUtils.extractExpirationAsLocalDateTime(tokenString))
				.user(user)
				.build();

		tokenRepository.save(token);
	}

	@Transactional
	public void revokeAllUserTokens(User user){
		// Получаем только валидные токены пользователя
		List<Token> validTokens = tokenRepository.findAllValidTokensByUser(user.getId());
		if(validTokens.isEmpty()) {
			return;
		}

		validTokens.forEach(token->{
			token.setRevoked(true);
		});

		tokenRepository.saveAll(validTokens);
	}

	@Transactional
	public Optional<Token> findByToken(String token){

		return tokenRepository.findByToken(token);
	}

	@Transactional
	public void revokeToken(String token){

		Optional<Token> tokenOptional = tokenRepository.findByToken(token);
		tokenOptional.ifPresent(t->{
			t.setRevoked(true);
			tokenRepository.save(t);
		});
	}

	@Transactional
	public boolean isTokenValid(String token){

		Optional<Token> tokenOptional = tokenRepository.findByToken(token);
		return tokenOptional.map(Token::isValid).orElse(false);
	}
}
