package kirillzhdanov.identityservice.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Класс для извлечения JWT токенов из HTTP запросов
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenExtractor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_TOKEN_LENGTH = 1000; // Максимальная длина токена
    private static final int MIN_TOKEN_LENGTH = 10; // Минимальная длина токена для базовой проверки

    /**
     * Извлекает JWT токен из заголовка запроса
     *
     * @param request HTTP запрос
     * @return JWT токен или null, если токен не найден
     */
    public String extractJwtFromRequest(@NonNull HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            return null;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        
        // Проверка на пустой токен и длину
        if (token.length() < MIN_TOKEN_LENGTH) {
            return null;
        }
        
        if (token.length() > MAX_TOKEN_LENGTH) {
            log.warn("Токен превышает максимально допустимую длину");
            return null;
        }
        
        return token;
    }
}
