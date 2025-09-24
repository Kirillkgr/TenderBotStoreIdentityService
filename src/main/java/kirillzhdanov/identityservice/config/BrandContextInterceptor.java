package kirillzhdanov.identityservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.util.BrandContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BrandContextInterceptor implements HandlerInterceptor {

    private final BrandRepository brandRepository;

    public BrandContextInterceptor(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long brandId = null;
        try {
            String header = request.getHeader("X-Brand-Id");
            if (header != null && !header.isBlank()) brandId = Long.valueOf(header);
        } catch (Exception ignored) {
        }
        if (brandId == null) {
            try {
                String q = request.getParameter("brand");
                if (q != null && !q.isBlank()) brandId = Long.valueOf(q);
            } catch (Exception ignored) {
            }
        }
        if (brandId == null) {
            // fallback: первый бренд из БД (для локальных тестов)
            var first = brandRepository.findAll(PageRequest.of(0, 1, Sort.by("id").ascending()));
            if (!first.isEmpty()) brandId = first.getContent().get(0).getId();
        }
        if (brandId != null) {
            BrandContextHolder.set(brandId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BrandContextHolder.clear();
    }
}
