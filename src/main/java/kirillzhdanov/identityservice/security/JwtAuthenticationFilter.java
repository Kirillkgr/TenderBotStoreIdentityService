package kirillzhdanov.identityservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр для аутентификации пользователя с использованием JWT токена
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenExtractor tokenExtractor;
    private final JwtAuthenticator authenticator;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Извлекаем JWT токен из заголовка
            String jwt = tokenExtractor.extractJwtFromRequest(request);

            // Если токен найден, обрабатываем его
            if (jwt != null) {
                authenticator.processJwtToken(request, jwt);
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT токен истек");
            authenticator.clearSecurityContext();
        } catch (SignatureException e) {
            log.warn("Неверная подпись JWT");
            authenticator.clearSecurityContext();
        } catch (MalformedJwtException e) {
            log.warn("Неверный формат JWT");
            authenticator.clearSecurityContext();
        } catch (UnsupportedJwtException e) {
            log.warn("Неподдерживаемый JWT");
            authenticator.clearSecurityContext();
        } catch (IllegalArgumentException e) {
            log.warn("Пустые утверждения JWT");
            authenticator.clearSecurityContext();
        } catch (Exception e) {
            // Логируем только тип исключения, без деталей
            log.error("Ошибка при аутентификации: {}", e.getClass().getSimpleName());
            authenticator.clearSecurityContext();
        } finally {
            // Продолжаем цепочку фильтров
            filterChain.doFilter(request, response);
        }
    }
}
