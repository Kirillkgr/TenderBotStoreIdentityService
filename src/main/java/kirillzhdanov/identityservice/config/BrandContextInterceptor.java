package kirillzhdanov.identityservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Устаревший перехватчик (deprecated), который раньше устанавливал брендовый контекст
 * из заголовков/параметров запроса. Теперь контекст бренда определяется из HttpOnly
 * cookie и читается через {@code ContextAccess}. Класс сохранён только на период миграции
 * и будет удалён позже.
 */
@Deprecated
@Component
public class BrandContextInterceptor implements HandlerInterceptor {

    private final BrandRepository brandRepository;

    public BrandContextInterceptor(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long brandId = ContextAccess.getBrandIdOrNull();
        if (brandId == null) {
            // fallback: первый бренд из БД (для локальных тестов)
            var first = brandRepository.findAll(PageRequest.of(0, 1, Sort.by("id").ascending()));
            if (!first.isEmpty()) brandId = first.getContent().get(0).getId();
        }
        // no-op: контекст бренда читается напрямую через ContextAccess
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // no-op
    }
}
