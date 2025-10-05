package kirillzhdanov.identityservice.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces presence of tenant context for protected endpoints.
 * Skips public endpoints and the context switch endpoint.
 */
public class ContextEnforcementFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            // Auth endpoints, которые не требуют tenant-контекста
            "/auth/v1/login",
            "/auth/v1/register",
            "/auth/v1/refresh",
            "/auth/v1/checkUsername",
            "/auth/v1/memberships",
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
            // Информация о пользователе
            "/auth/v1/whoami",
            "/order/v1/my",
            // Корзина не зависит от tenant-контекста
            "/cart",
            // Список заказов доступен без tenant-контекста (возвращает пусто без членства)
            "/order/v1",
            "/public/",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/status"
    );

    private boolean isPublic(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;
        // Allow first-time brand creation without tenant context
        if (HttpMethod.POST.matches(request.getMethod()) && "/auth/v1/brands".equals(uri)) return true;
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
        if (isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // Enforce only for authenticated requests
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated();
        if (authenticated) {
            // Require context for authenticated, protected requests; if not set -> 403
            if (TenantContext.getMasterId() == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
