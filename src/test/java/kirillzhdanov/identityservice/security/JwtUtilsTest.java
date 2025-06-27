package kirillzhdanov.identityservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class JwtUtilsTest {

	@InjectMocks
	private JwtUtils jwtUtils;

	private User testUser;

	private CustomUserDetails customUserDetails;

	private String accessToken;

	private String refreshToken;

	private String expiredToken;

	private String invalidSignatureToken;

	private String malformedToken;

	@BeforeEach
	void setUp(){
		// Устанавливаем значения для полей JwtUtils
		ReflectionTestUtils.setField(jwtUtils, "secret", "test_secret_key_for_jwt_token_that_is_long_enough");
		ReflectionTestUtils.setField(jwtUtils, "accessTokenExpiration", 3600000L); // 1 час
		ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpiration", 2592000000L); // 30 дней

		// Создаем тестового пользователя с ролями и брендами
		Role userRole = new Role();
		userRole.setId(1L);
		userRole.setName(Role.RoleName.USER);

		Role adminRole = new Role();
		adminRole.setId(2L);
		adminRole.setName(Role.RoleName.ADMIN);

		Brand brand1 = new Brand();
		brand1.setId(1L);
		brand1.setName("TestBrand1");

		Brand brand2 = new Brand();
		brand2.setId(2L);
		brand2.setName("TestBrand2");

		testUser = User.builder().id(1L).username("testuser").password("encodedPassword").roles(new HashSet<>(Arrays.asList(userRole, adminRole))).brands(new HashSet<>(Arrays.asList(brand1, brand2))).build();

		customUserDetails = new CustomUserDetails(testUser);

		// Генерируем токены для тестов
		accessToken = jwtUtils.generateAccessToken(customUserDetails);
		refreshToken = jwtUtils.generateRefreshToken(customUserDetails);

		// Создаем истекший токен с помощью мока даты вместо отрицательного срока действия
		Date pastDate = new Date(System.currentTimeMillis() - 3600000L); // 1 час назад
		Map<String, Object> claims = new HashMap<>();
		claims.put("tokenType", Token.TokenType.ACCESS.name());
		claims.put("userId", testUser.getId());
		claims.put("brandIds", testUser.getBrands().stream().map(Brand::getId).collect(Collectors.toList()));
		claims.put("roles", testUser.getRoles().stream().map(role->role.getName().name()).collect(Collectors.toList()));

		expiredToken = Jwts.builder().setClaims(claims).setSubject(customUserDetails.getUsername()).setIssuedAt(pastDate).setExpiration(pastDate) // Устанавливаем дату истечения в прошлом
				               .signWith(SignatureAlgorithm.HS256, "test_secret_key_for_jwt_token_that_is_long_enough").compact();

		// Создаем токен с неверной подписью
		invalidSignatureToken = accessToken.substring(0, accessToken.lastIndexOf('.') + 1) + "invalid_signature";

		// Создаем некорректный токен
		malformedToken = "malformed.jwt.token";
	}

	@Test
	@DisplayName("Генерация токена доступа - успешно")
	void generateAccessToken_Success(){
		// Проверка
		assertNotNull(accessToken);
		assertFalse(accessToken.isEmpty());

		// Проверяем, что токен содержит правильные данные
		String username = jwtUtils.extractUsername(accessToken);
		assertEquals(testUser.getUsername(), username);

		Token.TokenType tokenType = jwtUtils.extractTokenType(accessToken);
		assertEquals(Token.TokenType.ACCESS, tokenType);

		Long userId = jwtUtils.extractUserId(accessToken);
		assertEquals(testUser.getId(), userId);

		List<Long> brandIds = jwtUtils.extractBrandIds(accessToken);
		assertEquals(2, brandIds.size());

		// Преобразуем в Set для сравнения содержимого независимо от порядка
		Set<Long> brandIdSet = new HashSet<>(brandIds);
		assertTrue(brandIdSet.contains(1));
		assertTrue(brandIdSet.contains(2));

		List<String> roles = jwtUtils.extractRoles(accessToken);
		assertEquals(2, roles.size());

		// Преобразуем в Set для сравнения содержимого независимо от порядка
		Set<String> roleSet = new HashSet<>(roles);
		assertTrue(roleSet.contains("USER"));
		assertTrue(roleSet.contains("ADMIN"));
	}

	@Test
	@DisplayName("Генерация токена обновления - успешно")
	void generateRefreshToken_Success(){
		// Проверка
		assertNotNull(refreshToken);
		assertTrue(!refreshToken.isEmpty());

		// Проверяем, что токен содержит правильные данные
		String username = jwtUtils.extractUsername(refreshToken);
		assertEquals(testUser.getUsername(), username);

		Token.TokenType tokenType = jwtUtils.extractTokenType(refreshToken);
		assertEquals(Token.TokenType.REFRESH, tokenType);

		// Refresh токен не должен содержать дополнительных данных
		Claims claims = ReflectionTestUtils.invokeMethod(jwtUtils, "extractAllClaims", refreshToken);
		assertNotNull(claims);
		assertFalse(claims.containsKey("userId"));
		assertFalse(claims.containsKey("brandIds"));
		assertFalse(claims.containsKey("roles"));
	}

	@Test
	@DisplayName("Извлечение имени пользователя из токена - успешно")
	void extractUsername_Success(){
		// Выполнение
		String username = jwtUtils.extractUsername(accessToken);

		// Проверка
		assertEquals(testUser.getUsername(), username);
	}

	@Test
	@DisplayName("Извлечение даты истечения из токена - успешно")
	void extractExpiration_Success(){
		// Выполнение
		Date expiration = jwtUtils.extractExpiration(accessToken);

		// Проверка
		assertNotNull(expiration);
		assertTrue(expiration.after(new Date()));
	}

	@Test
	@DisplayName("Извлечение даты истечения как LocalDateTime - успешно")
	void extractExpirationAsLocalDateTime_Success(){
		// Выполнение
		LocalDateTime expiration = jwtUtils.extractExpirationAsLocalDateTime(accessToken);

		// Проверка
		assertNotNull(expiration);
		assertTrue(expiration.isAfter(LocalDateTime.now()));
	}

	@Test
	@DisplayName("Извлечение типа токена - успешно")
	void extractTokenType_Success(){
		// Выполнение
		Token.TokenType accessTokenType = jwtUtils.extractTokenType(accessToken);
		Token.TokenType refreshTokenType = jwtUtils.extractTokenType(refreshToken);

		// Проверка
		assertEquals(Token.TokenType.ACCESS, accessTokenType);
		assertEquals(Token.TokenType.REFRESH, refreshTokenType);
	}

	@Test
	@DisplayName("Извлечение ID пользователя - успешно")
	void extractUserId_Success(){
		// Выполнение
		Long userId = jwtUtils.extractUserId(accessToken);

		// Проверка
		assertEquals(testUser.getId(), userId);
	}

	@Test
	@DisplayName("Извлечение ID брендов - успешно")
	void extractBrandIds_Success(){
		// Выполнение
		List<Long> brandIds = jwtUtils.extractBrandIds(accessToken);

		// Проверка
		assertEquals(2, brandIds.size());

		// Преобразуем в Set для сравнения содержимого независимо от порядка
		Set<Long> brandIdSet = new HashSet<>(brandIds);
		assertTrue(brandIdSet.contains(1));
		assertTrue(brandIdSet.contains(2));
	}

	@Test
	@DisplayName("Извлечение ролей - успешно")
	void extractRoles_Success(){
		// Выполнение
		List<String> roles = jwtUtils.extractRoles(accessToken);

		// Проверка
		assertEquals(2, roles.size());

		// Преобразуем в Set для сравнения содержимого независимо от порядка
		Set<String> roleSet = new HashSet<>(roles);
		assertTrue(roleSet.contains("USER"));
		assertTrue(roleSet.contains("ADMIN"));
	}

	@Test
	@DisplayName("Валидация токена - успешно")
	void validateToken_Success(){
		// Выполнение
		boolean isValid = jwtUtils.validateToken(accessToken, customUserDetails);

		// Проверка
		assertTrue(isValid);
	}

	@Test
	@DisplayName("Валидация токена - неверное имя пользователя")
	void validateToken_WrongUsername(){
		// Подготовка
		UserDetails wrongUser = new CustomUserDetails(User.builder().id(2L).username("wronguser").password("encodedPassword").roles(new HashSet<>()).brands(new HashSet<>()).build());

		// Выполнение
		boolean isValid = jwtUtils.validateToken(accessToken, wrongUser);

		// Проверка
		assertFalse(isValid);
	}

	@Test
	@DisplayName("Валидация токена - истекший токен")
	void validateToken_ExpiredToken(){
		// Выполнение и проверка
		try {
			boolean isValid = jwtUtils.validateToken(expiredToken, customUserDetails);
			assertFalse(isValid);
		} catch(ExpiredJwtException e) {
			// Если выбрасывается исключение, то тест также проходит
			assertTrue(true);
		}
	}

	@Test
	@DisplayName("Валидация токена - неверная подпись")
	void validateToken_InvalidSignature(){
		// Выполнение и проверка
		assertThrows(SignatureException.class, ()->jwtUtils.validateToken(invalidSignatureToken, customUserDetails));
	}

	@Test
	@DisplayName("Валидация токена - некорректный формат")
	void validateToken_MalformedToken(){
		// Выполнение и проверка
		assertThrows(MalformedJwtException.class, ()->jwtUtils.validateToken(malformedToken, customUserDetails));
	}

	@Test
	@DisplayName("Извлечение всех утверждений из токена - успешно")
	void extractAllClaims_Success() throws Exception{
		// Выполнение
		Claims claims = ReflectionTestUtils.invokeMethod(jwtUtils, "extractAllClaims", accessToken);

		// Проверка
		assertNotNull(claims);
		assertEquals(testUser.getUsername(), claims.getSubject());
		assertEquals(Token.TokenType.ACCESS.name(), claims.get("tokenType"));
		assertEquals(testUser.getId().longValue(), ((Number) claims.get("userId")).longValue());
	}

	@Test
	@DisplayName("Проверка срока действия токена - действительный токен")
	void isTokenExpired_ValidToken() throws Exception{
		// Выполнение
		Boolean isExpired = ReflectionTestUtils.invokeMethod(jwtUtils, "isTokenExpired", accessToken);

		// Проверка
		assertNotNull(isExpired);
		assertFalse(isExpired);
	}

	@Test
	@DisplayName("Проверка срока действия токена - истекший токен")
	void isTokenExpired_ExpiredToken(){
		// Выполнение
		Boolean isExpired = ReflectionTestUtils.invokeMethod(jwtUtils, "isTokenExpired", expiredToken);

		// Проверка
		assertNotNull(isExpired);
		assertTrue(isExpired);
	}
}
