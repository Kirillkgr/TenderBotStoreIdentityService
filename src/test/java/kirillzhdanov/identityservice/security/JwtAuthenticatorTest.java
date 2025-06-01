package kirillzhdanov.identityservice.security;

import jakarta.servlet.http.HttpServletRequest;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticatorTest {

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private TokenService tokenService;

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private JwtAuthenticator jwtAuthenticator;

	private CustomUserDetails userDetails;

	private String validAccessToken;

	private String refreshToken;

	private String invalidToken;

	@BeforeEach
	void setUp(){
		// Очищаем контекст безопасности перед каждым тестом
		SecurityContextHolder.clearContext();

		// Создаем тестового пользователя
		Role userRole = new Role();
		userRole.setId(1L);
		userRole.setName(Role.RoleName.USER);

		User testUser = User.builder()
								.id(1L)
								.username("testuser")
								.password("encodedPassword")
								.roles(new HashSet<>(Collections.singletonList(userRole)))
								.brands(new HashSet<>())
								.build();

		userDetails = new CustomUserDetails(testUser);
		validAccessToken = "valid-access-token";
		refreshToken = "refresh-token";
		invalidToken = "invalid-token";
	}

	@Test
	@DisplayName("Обработка валидного JWT токена - успешно")
	void processJwtToken_ValidToken_Success(){
		// Подготовка
		when(jwtUtils.extractTokenType(validAccessToken)).thenReturn(Token.TokenType.ACCESS);
		when(jwtUtils.extractUsername(validAccessToken)).thenReturn("testuser");
		when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
		when(jwtUtils.validateToken(validAccessToken, userDetails)).thenReturn(true);
		when(tokenService.isTokenValid(validAccessToken)).thenReturn(true);
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, validAccessToken);

		// Проверка
		assertTrue(result);

		// Проверяем, что пользователь аутентифицирован
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(authentication);
		assertTrue(authentication.isAuthenticated());
		assertEquals(userDetails, authentication.getPrincipal());

		verify(jwtUtils).extractTokenType(validAccessToken);
		verify(jwtUtils).extractUsername(validAccessToken);
		verify(userDetailsService).loadUserByUsername("testuser");
		verify(jwtUtils).validateToken(validAccessToken, userDetails);
		verify(tokenService).isTokenValid(validAccessToken);
	}

	@Test
	@DisplayName("Обработка JWT токена - неверный тип токена")
	void processJwtToken_WrongTokenType(){
		// Подготовка
		when(jwtUtils.extractTokenType(refreshToken)).thenReturn(Token.TokenType.REFRESH);

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, refreshToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(refreshToken);
		verify(jwtUtils, never()).extractUsername(anyString());
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("Обработка JWT токена - ошибка при извлечении типа токена")
	void processJwtToken_ErrorExtractingTokenType(){
		// Подготовка
		when(jwtUtils.extractTokenType(invalidToken)).thenThrow(new RuntimeException("Ошибка при извлечении типа токена"));

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, invalidToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(invalidToken);
		verify(jwtUtils, never()).extractUsername(anyString());
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("Обработка JWT токена - пустое имя пользователя")
	void processJwtToken_EmptyUsername(){
		// Подготовка
		when(jwtUtils.extractTokenType(validAccessToken)).thenReturn(Token.TokenType.ACCESS);
		when(jwtUtils.extractUsername(validAccessToken)).thenReturn("");

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, validAccessToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(validAccessToken);
		verify(jwtUtils).extractUsername(validAccessToken);
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("Обработка JWT токена - ошибка при извлечении имени пользователя")
	void processJwtToken_ErrorExtractingUsername(){
		// Подготовка
		when(jwtUtils.extractTokenType(validAccessToken)).thenReturn(Token.TokenType.ACCESS);
		when(jwtUtils.extractUsername(validAccessToken)).thenThrow(new RuntimeException("Ошибка при извлечении имени пользователя"));

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, validAccessToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(validAccessToken);
		verify(jwtUtils).extractUsername(validAccessToken);
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("Обработка JWT токена - пользователь не найден")
	void processJwtToken_UserNotFound(){
		// Подготовка
		when(jwtUtils.extractTokenType(validAccessToken)).thenReturn(Token.TokenType.ACCESS);
		when(jwtUtils.extractUsername(validAccessToken)).thenReturn("testuser");
		when(userDetailsService.loadUserByUsername("testuser")).thenThrow(new UsernameNotFoundException("Пользователь не найден"));

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, validAccessToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(validAccessToken);
		verify(jwtUtils).extractUsername(validAccessToken);
		verify(userDetailsService).loadUserByUsername("testuser");
		verify(jwtUtils, never()).validateToken(anyString(), any());
	}

	@Test
	@DisplayName("Обработка JWT токена - UserDetailsService вернул null")
	void processJwtToken_UserDetailsServiceReturnsNull(){
		// Подготовка
		when(jwtUtils.extractTokenType(validAccessToken)).thenReturn(Token.TokenType.ACCESS);
		when(jwtUtils.extractUsername(validAccessToken)).thenReturn("testuser");
		when(userDetailsService.loadUserByUsername("testuser")).thenReturn(null);

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, validAccessToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(validAccessToken);
		verify(jwtUtils).extractUsername(validAccessToken);
		verify(userDetailsService).loadUserByUsername("testuser");
		verify(jwtUtils, never()).validateToken(anyString(), any());
	}

	@Test
	@DisplayName("Обработка JWT токена - невалидный JWT токен")
	void processJwtToken_InvalidJwtToken(){
		// Подготовка
		when(jwtUtils.extractTokenType(validAccessToken)).thenReturn(Token.TokenType.ACCESS);
		when(jwtUtils.extractUsername(validAccessToken)).thenReturn("testuser");
		when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
		when(jwtUtils.validateToken(validAccessToken, userDetails)).thenReturn(false);

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, validAccessToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(validAccessToken);
		verify(jwtUtils).extractUsername(validAccessToken);
		verify(userDetailsService).loadUserByUsername("testuser");
		verify(jwtUtils).validateToken(validAccessToken, userDetails);
		verify(tokenService, never()).isTokenValid(anyString());
	}

	@Test
	@DisplayName("Обработка JWT токена - токен отозван в базе данных")
	void processJwtToken_TokenRevokedInDatabase(){
		// Подготовка
		when(jwtUtils.extractTokenType(validAccessToken)).thenReturn(Token.TokenType.ACCESS);
		when(jwtUtils.extractUsername(validAccessToken)).thenReturn("testuser");
		when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
		when(jwtUtils.validateToken(validAccessToken, userDetails)).thenReturn(true);
		when(tokenService.isTokenValid(validAccessToken)).thenReturn(false);

		// Выполнение
		boolean result = jwtAuthenticator.processJwtToken(request, validAccessToken);

		// Проверка
		assertFalse(result);
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtUtils).extractTokenType(validAccessToken);
		verify(jwtUtils).extractUsername(validAccessToken);
		verify(userDetailsService).loadUserByUsername("testuser");
		verify(jwtUtils).validateToken(validAccessToken, userDetails);
		verify(tokenService).isTokenValid(validAccessToken);
	}

	@Test
	@DisplayName("Очистка контекста безопасности")
	void clearSecurityContext(){
		// Подготовка
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
		);

		// Проверяем, что аутентификация установлена
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());

		// Выполнение
		jwtAuthenticator.clearSecurityContext();

		// Проверка
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
