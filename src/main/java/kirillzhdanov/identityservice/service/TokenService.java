package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.*;
import kirillzhdanov.identityservice.repository.TokenRepository;
import kirillzhdanov.identityservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TokenService {

	private final TokenRepository tokenRepository;

	private final JwtUtils jwtUtils;

	@Transactional
	public void saveToken(String tokenValue, Token.TokenType tokenType, User user) {
		// Проверяем входные параметры
		if (tokenValue == null || tokenType == null || user == null) {
			throw new IllegalArgumentException("Параметры токена не могут быть null");
		}

		// Проверяем, существует ли уже такой токен
		Optional<Token> existingToken = tokenRepository.findByToken(tokenValue);
		if (existingToken.isPresent()) {
			// Если токен существует, но отозван или истек, обновляем его
			Token token = existingToken.get();
			if (token.isRevoked() || token.getExpiryDate()
										  .isBefore(LocalDateTime.now())) {
				token.setRevoked(false);
				token.setExpiryDate(jwtUtils.extractExpirationAsLocalDateTime(tokenValue));
				tokenRepository.save(token);
				return;
			}
			// Если токен уже существует и действителен, ничего не делаем
			return;
		}

		// Создаем новый токен
		Token token = Token.builder()
						   .token(tokenValue)
						   .tokenType(tokenType)
						   .revoked(false)
						   .expiryDate(jwtUtils.extractExpirationAsLocalDateTime(tokenValue))
						   .user(user)
						   .build();

		// Сохраняем токен
		tokenRepository.save(token);
	}

	@Transactional(readOnly = true)
	public Optional<Token> findByToken(String tokenValue) {

		return tokenRepository.findByToken(tokenValue);
	}

	@Transactional
	public void revokeToken(String tokenValue) {

		Optional<Token> tokenOptional = tokenRepository.findByToken(tokenValue);
		tokenOptional.ifPresent(token -> {
			token.setRevoked(true);
			tokenRepository.save(token);
		});
	}

	@Transactional
	public void revokeAllUserTokens(User user) {

		List<Token> validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());
		if (validUserTokens.isEmpty()) {
			return;
		}

		validUserTokens.forEach(token -> {
			token.setRevoked(true);
			tokenRepository.save(token);
		});
	}

	@Transactional(readOnly = true)
	public boolean isTokenValid(String tokenValue) {

		Optional<Token> tokenOptional = tokenRepository.findByToken(tokenValue);
		return tokenOptional.map(Token::isValid)
							.orElse(false);
	}

	@Transactional
	public void cleanupExpiredTokens() {

		LocalDateTime now = LocalDateTime.now();
		List<Token> expiredTokens = tokenRepository.findAllByExpiryDateBefore(now);
		tokenRepository.deleteAll(expiredTokens);
	}
}
