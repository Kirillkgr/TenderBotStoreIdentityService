package kirillzhdanov.identityservice.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kirillzhdanov.identityservice.util.HmacSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Фильтр читает подписанную HttpOnly cookie "ctx" и наполняет контекст запроса.
 * Формат cookie:
 * ctx=<base64url(JSON)>.<base64url(HMAC)>
 * где JSON может содержать поля: masterId, brandId, pickupPointId, issuedAt.
 * Подпись HMAC защищает от подделки значений.
 * Жизненный цикл:
 * - ДО передачи управления контроллеру фильтр пытается разобрать cookie и
 * положить значения в {@link ContextResolver} (ThreadLocal на время запроса).
 * - ПОСЛЕ обработки запроса контекст обязательно очищается (finally).
 */
@Component
public class CtxCookieFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ctx.cookie.secret:change-me}")
    private String secret;

    /**
     * Основной метод фильтра: разбирает cookie "ctx", проверяет подпись
     * и устанавливает контекст запроса. При любой ошибке контекст просто не устанавливается.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("ctx".equals(c.getName())) {
                        applyContext(c.getValue());
                        break;
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextResolver.clear();
        }
    }

    /**
     * Разбирает полезную нагрузку cookie, проверяет HMAC-подпись и устанавливает контекст.
     */
    private void applyContext(String raw) {
        if (raw == null || raw.isEmpty()) return;
        int dot = raw.lastIndexOf('.');
        if (dot <= 0) return;
        String payload = raw.substring(0, dot);
        String signature = raw.substring(dot + 1);
        HmacSigner signer = new HmacSigner(secret);
        if (!signer.verifyBase64Url(payload, signature)) {
            return; // invalid signature
        }
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(payload);
            JsonNode node = mapper.readTree(new String(jsonBytes, StandardCharsets.UTF_8));
            Long masterId = node.hasNonNull("masterId") ? node.get("masterId").asLong() : null;
            Long brandId = node.hasNonNull("brandId") ? node.get("brandId").asLong() : null;
            Long pickupPointId = node.hasNonNull("pickupPointId") ? node.get("pickupPointId").asLong() : null;
            Long issuedAt = node.hasNonNull("issuedAt") ? node.get("issuedAt").asLong() : null;
            ContextResolver.set(new ContextResolver.Ctx(masterId, brandId, pickupPointId, issuedAt));
        } catch (Exception ignored) {
        }
    }
}
