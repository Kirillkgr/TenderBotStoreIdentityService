package kirillzhdanov.identityservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenExtractorTest {

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private JwtTokenExtractor jwtTokenExtractor;

	@Test
	@DisplayName("Извлечение JWT токена из заголовка - успешно")
	void extractToken_ValidHeader_Success() {
		// Подготовка
		String token = "valid-jwt-token";
		when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

		// Выполнение
		String extractedToken = jwtTokenExtractor.extractJwtFromRequest(request);

		// Проверка
		assertEquals(token, extractedToken);
	}

	@Test
	@DisplayName("Извлечение JWT токена - заголовок отсутствует")
	void extractToken_NoHeader_ReturnsNull() {
		// Подготовка
		when(request.getHeader("Authorization")).thenReturn(null);

		// Выполнение
		String extractedToken = jwtTokenExtractor.extractJwtFromRequest(request);

		// Проверка
		assertNull(extractedToken);
	}

	@Test
	@DisplayName("Извлечение JWT токена - неверный формат заголовка")
	void extractToken_InvalidHeaderFormat_ReturnsNull() {
		// Подготовка
		when(request.getHeader("Authorization")).thenReturn("Invalid-format");

		// Выполнение
		String extractedToken = jwtTokenExtractor.extractJwtFromRequest(request);

		// Проверка
		assertNull(extractedToken);
	}

	@Test
	@DisplayName("Извлечение JWT токена - пустой токен")
	void extractToken_EmptyToken_ReturnsNull() {
		// Подготовка
		when(request.getHeader("Authorization")).thenReturn("Bearer ");

		// Выполнение
		String extractedToken = jwtTokenExtractor.extractJwtFromRequest(request);

		// Проверка
		assertNull(extractedToken);
	}

	@Test
	@DisplayName("Извлечение JWT токена - слишком короткий токен")
	void extractToken_TooShortToken_ReturnsNull() {
		// Подготовка
		when(request.getHeader("Authorization")).thenReturn("Bearer 123");

		// Выполнение
		String extractedToken = jwtTokenExtractor.extractJwtFromRequest(request);

		// Проверка
		assertNull(extractedToken);
	}
}
