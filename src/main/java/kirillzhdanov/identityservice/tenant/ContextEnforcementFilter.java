package kirillzhdanov.identityservice.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Фильтр-предохранитель: убеждается, что для защищённых запросов установлен контекст (brand/master/pickup).
 * <p>
 * Простыми словами: если пользователь авторизован и обращается к закрытой части API,
 * у запроса должен быть "рабочий контекст" (каким брендом и точкой он сейчас пользуется).
 * Публичные пути пропускаются без проверки.
 */
public class ContextEnforcementFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ContextEnforcementFilter.class);

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            // Auth endpoints, которые не требуют tenant-контекста
            "/auth/v1/login",
            "/auth/v1/register",
            "/auth/v1/refresh",
            "/auth/v1/checkUsername",
            "/auth/v1/memberships",
            "/auth/v1/context",
            "/auth/v1/context/switch",
            "/auth/v1/revoke",
            "/auth/v1/revoke-all",
            "/auth/v1/logout",
            "/auth/v1/logout/all",
            // User-пути не завязаны на tenant-контекст
            "/user/v1/",
            // Публичные/документация
            "/oauth2/authorization",
            "/login/oauth2/code",
            "/notifications/longpoll",
            "/menu/",
            "/doc/swagger-ui",
            "/doc/swagger",
            "/doc/api-docs",
            "/public/",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            // Информация о пользователе
            "/auth/v1/whoami",
            "/order/v1/my",
            // Корзина не зависит от tenant-контекста
            "/cart",
            // Список заказов доступен без tenant-контекста (возвращает пусто без членства)
            "/order/v1",
            // кастомные пути springdoc
            "/status"
    );

    private boolean isPublic(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // Allow first-time brand creation without tenant context
        // Allow listing brands for authenticated users without tenant context (fallback master will be derived)
        for (String p : PUBLIC_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        // Allow GET menu endpoints explicitly
        return request.getMethod().equals("GET") && (uri.startsWith("/menu/") || "/orders".equals(uri));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Если путь публичный — пропускаем без проверок
        if (isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // Проверку контекста выполняем только для авторизованных запросов
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated();
        if (authenticated) {
            // Требуем наличие контекста: либо legacy (на время миграции), либо cookie-контекст
            boolean hasLegacy = TenantContext.getMasterId() != null;
            boolean hasCookieCtx = ContextResolver.current() != null && ContextResolver.current().masterId() != null;
            if (!hasLegacy && !hasCookieCtx) {
                String user = (auth != null ? auth.getName() : "<anonymous>");
                log.warn("ContextEnforcement: missing tenant context for {} {} (user={}) -> 403", request.getMethod(), request.getRequestURI(), user);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
