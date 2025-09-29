package kirillzhdanov.identityservice.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Временный фильтр для TASK-1.2: берёт masterId из заголовка X-Master-Id
 * и записывает его в TenantContext. Полная валидация membership будет добавлена в TASK-1.3.
 */
public class TenantContextFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-Master-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER);
            if (header != null && !header.isBlank()) {
                try {
                    Long masterId = Long.parseLong(header.trim());
                    TenantContext.setMasterId(masterId);
                } catch (NumberFormatException ignored) {
                    // Неверный формат заголовка — контекст не устанавливаем
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
