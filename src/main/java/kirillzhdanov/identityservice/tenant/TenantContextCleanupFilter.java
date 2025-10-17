package kirillzhdanov.identityservice.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Гарантирует отсутствие протечки TenantContext (legacy ThreadLocal) между запросами.
 * Очищает контекст в начале и в конце каждого запроса.
 */
@Component
public class TenantContextCleanupFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // На всякий случай очищаем перед обработкой
        try {
            TenantContext.clear();
        } catch (Exception ignored) {
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // И обязательно очищаем после
            try {
                TenantContext.clear();
            } catch (Exception ignored) {
            }
        }
    }
}
