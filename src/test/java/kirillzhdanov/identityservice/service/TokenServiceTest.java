package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.TokenRepository;
import kirillzhdanov.identityservice.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

	@Mock
	private TokenRepository tokenRepository;

	@Mock
	private JwtUtils jwtUtils;

	@InjectMocks
	private TokenService tokenService;

	private User testUser;

	private Token accessToken;

	private Token refreshToken;

	private Token expiredToken;

	private Token revokedToken;

	@BeforeEach
	void setUp(){
		// Создаем тестового пользователя
		Role userRole = new Role();
		userRole.setId(1L);
		userRole.setName(Role.RoleName.USER);

		testUser = User.builder()
						   .id(1L)
						   .username("testuser")
						   .password("encodedPassword")
						   .roles(new HashSet<>(Collections.singletonList(userRole)))
						   .brands(new HashSet<>())
						   .build();

		// Создаем тестовые токены
		LocalDateTime now = LocalDateTime.now();

		accessToken = Token.builder()
							  .id(1L)
							  .token("access-token-123")
							  .tokenType(Token.TokenType.ACCESS)
							  .revoked(false)
							  .expiryDate(now.plusHours(1))
							  .user(testUser)
							  .build();

		refreshToken = Token.builder()
							   .id(2L)
							   .token("refresh-token-123")
							   .tokenType(Token.TokenType.REFRESH)
							   .revoked(false)
							   .expiryDate(now.plusDays(7))
							   .user(testUser)
							   .build();

		expiredToken = Token.builder()
							   .id(3L)
							   .token("expired-token-123")
							   .tokenType(Token.TokenType.ACCESS)
							   .revoked(false)
							   .expiryDate(now.minusHours(1))
							   .user(testUser)
							   .build();

		revokedToken = Token.builder()
							   .id(4L)
							   .token("revoked-token-123")
							   .tokenType(Token.TokenType.ACCESS)
							   .revoked(true)
							   .expiryDate(now.plusHours(1))
							   .user(testUser)
							   .build();
	}

	@Test
	@DisplayName("Сохранение нового токена - успешно")
	void saveToken_NewToken_Success(){
		// Подготовка
		String tokenValue = "new-token-123";
		when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());
		when(jwtUtils.extractExpirationAsLocalDateTime(tokenValue)).thenReturn(LocalDateTime.now().plusHours(1));
		when(tokenRepository.save(any(Token.class))).thenAnswer(invocation->invocation.getArgument(0));

		// Выполнение
		tokenService.saveToken(tokenValue, Token.TokenType.ACCESS, testUser);

		// Проверка
		verify(tokenRepository).findByToken(tokenValue);
		verify(jwtUtils).extractExpirationAsLocalDateTime(tokenValue);
		verify(tokenRepository).save(argThat(token->
													 token.getToken().equals(tokenValue) &&
															 token.getTokenType() == Token.TokenType.ACCESS &&
															 token.getUser() == testUser &&
															 !token.isRevoked()));
	}

	@Test
	@DisplayName("Сохранение существующего отозванного токена - обновление")
	void saveToken_ExistingRevokedToken_Update(){
		// Подготовка
		String tokenValue = "revoked-token-123";
		LocalDateTime newExpiry = LocalDateTime.now().plusHours(1);

		when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(revokedToken));
		when(jwtUtils.extractExpirationAsLocalDateTime(tokenValue)).thenReturn(newExpiry);
		when(tokenRepository.save(any(Token.class))).thenAnswer(invocation->invocation.getArgument(0));

		// Выполнение
		tokenService.saveToken(tokenValue, Token.TokenType.ACCESS, testUser);

		// Проверка
		verify(tokenRepository).findByToken(tokenValue);
		verify(jwtUtils).extractExpirationAsLocalDateTime(tokenValue);
		verify(tokenRepository).save(argThat(token->
													 token.getToken().equals(tokenValue) &&
															 !token.isRevoked() &&
															 token.getExpiryDate().equals(newExpiry)));
	}

	@Test
	@DisplayName("Сохранение существующего действительного токена - без изменений")
	void saveToken_ExistingValidToken_NoChange(){
		// Подготовка
		String tokenValue = "access-token-123";
		when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(accessToken));

		// Выполнение
		tokenService.saveToken(tokenValue, Token.TokenType.ACCESS, testUser);

		// Проверка
		verify(tokenRepository).findByToken(tokenValue);
		verify(jwtUtils, never()).extractExpirationAsLocalDateTime(anyString());
		verify(tokenRepository, never()).save(any(Token.class));
	}

	@Test
	@DisplayName("Сохранение токена с null параметрами - исключение")
	void saveToken_NullParameters_ThrowsException(){
		// Проверка
		assertThrows(IllegalArgumentException.class, ()->tokenService.saveToken(null, Token.TokenType.ACCESS, testUser));
		assertThrows(IllegalArgumentException.class, ()->tokenService.saveToken("token", null, testUser));
		assertThrows(IllegalArgumentException.class, ()->tokenService.saveToken("token", Token.TokenType.ACCESS, null));

		verify(tokenRepository, never()).findByToken(anyString());
		verify(tokenRepository, never()).save(any(Token.class));
	}

	@Test
	@DisplayName("Поиск токена по значению - успешно")
	void findByToken_Success(){
		// Подготовка
		when(tokenRepository.findByToken("access-token-123")).thenReturn(Optional.of(accessToken));

		// Выполнение
		Optional<Token> result = tokenService.findByToken("access-token-123");

		// Проверка
		assertTrue(result.isPresent());
		assertEquals(accessToken, result.get());
		verify(tokenRepository).findByToken("access-token-123");
	}

	@Test
	@DisplayName("Поиск токена по значению - токен не найден")
	void findByToken_NotFound(){
		// Подготовка
		when(tokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

		// Выполнение
		Optional<Token> result = tokenService.findByToken("non-existent-token");

		// Проверка
		assertFalse(result.isPresent());
		verify(tokenRepository).findByToken("non-existent-token");
	}

	@Test
	@DisplayName("Отзыв токена - успешно")
	void revokeToken_Success(){
		// Подготовка
		when(tokenRepository.findByToken("access-token-123")).thenReturn(Optional.of(accessToken));
		when(tokenRepository.save(any(Token.class))).thenAnswer(invocation->invocation.getArgument(0));

		// Выполнение
		tokenService.revokeToken("access-token-123");

		// Проверка
		verify(tokenRepository).findByToken("access-token-123");
		verify(tokenRepository).save(argThat(Token::isRevoked));
	}

	@Test
	@DisplayName("Отзыв токена - токен не найден")
	void revokeToken_NotFound(){
		// Подготовка
		when(tokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

		// Выполнение
		tokenService.revokeToken("non-existent-token");

		// Проверка
		verify(tokenRepository).findByToken("non-existent-token");
		verify(tokenRepository, never()).save(any(Token.class));
	}

	@Test
	@DisplayName("Отзыв всех токенов пользователя - успешно")
	void revokeAllUserTokens_Success(){
		// Подготовка
		List<Token> validTokens = Arrays.asList(accessToken, refreshToken);
		when(tokenRepository.findAllValidTokensByUser(1L)).thenReturn(validTokens);
		when(tokenRepository.save(any(Token.class))).thenAnswer(invocation->invocation.getArgument(0));

		// Выполнение
		tokenService.revokeAllUserTokens(testUser);

		// Проверка
		verify(tokenRepository).findAllValidTokensByUser(1L);
		verify(tokenRepository, times(2)).save(argThat(Token::isRevoked));
	}

	@Test
	@DisplayName("Отзыв всех токенов пользователя - нет действительных токенов")
	void revokeAllUserTokens_NoValidTokens(){
		// Подготовка
		when(tokenRepository.findAllValidTokensByUser(1L)).thenReturn(Collections.emptyList());

		// Выполнение
		tokenService.revokeAllUserTokens(testUser);

		// Проверка
		verify(tokenRepository).findAllValidTokensByUser(1L);
		verify(tokenRepository, never()).save(any(Token.class));
	}

	@Test
	@DisplayName("Проверка валидности токена - действительный токен")
	void isTokenValid_ValidToken(){
		// Подготовка
		when(tokenRepository.findByToken("access-token-123")).thenReturn(Optional.of(accessToken));

		// Выполнение
		boolean result = tokenService.isTokenValid("access-token-123");

		// Проверка
		assertTrue(result);
		verify(tokenRepository).findByToken("access-token-123");
	}

	@Test
	@DisplayName("Проверка валидности токена - отозванный токен")
	void isTokenValid_RevokedToken(){
		// Подготовка
		when(tokenRepository.findByToken("revoked-token-123")).thenReturn(Optional.of(revokedToken));

		// Выполнение
		boolean result = tokenService.isTokenValid("revoked-token-123");

		// Проверка
		assertFalse(result);
		verify(tokenRepository).findByToken("revoked-token-123");
	}

	@Test
	@DisplayName("Проверка валидности токена - истекший токен")
	void isTokenValid_ExpiredToken(){
		// Подготовка
		when(tokenRepository.findByToken("expired-token-123")).thenReturn(Optional.of(expiredToken));

		// Выполнение
		boolean result = tokenService.isTokenValid("expired-token-123");

		// Проверка
		assertFalse(result);
		verify(tokenRepository).findByToken("expired-token-123");
	}

	@Test
	@DisplayName("Проверка валидности токена - токен не найден")
	void isTokenValid_TokenNotFound(){
		// Подготовка
		when(tokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

		// Выполнение
		boolean result = tokenService.isTokenValid("non-existent-token");

		// Проверка
		assertFalse(result);
		verify(tokenRepository).findByToken("non-existent-token");
	}

	@Test
	@DisplayName("Очистка истекших токенов - успешно")
	void cleanupExpiredTokens_Success(){
		// Подготовка
		List<Token> expiredTokens = Collections.singletonList(expiredToken);
		when(tokenRepository.findAllByExpiryDateBefore(any(LocalDateTime.class))).thenReturn(expiredTokens);
		doNothing().when(tokenRepository).deleteAll(anyList());

		// Выполнение
		tokenService.cleanupExpiredTokens();

		// Проверка
		verify(tokenRepository).findAllByExpiryDateBefore(any(LocalDateTime.class));
		verify(tokenRepository).deleteAll(expiredTokens);
	}

	@Test
	@DisplayName("Очистка истекших токенов - нет истекших токенов")
	void cleanupExpiredTokens_NoExpiredTokens(){
		// Подготовка
		when(tokenRepository.findAllByExpiryDateBefore(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

		// Выполнение
		tokenService.cleanupExpiredTokens();

		// Проверка
		verify(tokenRepository).findAllByExpiryDateBefore(any(LocalDateTime.class));
		verify(tokenRepository).deleteAll(Collections.emptyList());
	}
}
