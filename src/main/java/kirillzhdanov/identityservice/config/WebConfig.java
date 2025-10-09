package kirillzhdanov.identityservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Устаревшая конфигурация (deprecated) для регистрации {@link BrandContextInterceptor}.
 * В текущей архитектуре контекст бренда читается из HttpOnly cookie через ContextAccess,
 * перехватчик не требуется. Класс сохранён на время миграции и будет удалён позже.
 */
@Deprecated
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BrandContextInterceptor brandContextInterceptor;
    private final Environment env;

    public WebConfig(BrandContextInterceptor brandContextInterceptor, Environment env) {
        this.brandContextInterceptor = brandContextInterceptor;
        this.env = env;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        boolean allowHeaders = Boolean.parseBoolean(env.getProperty("ALLOW_CONTEXT_HEADERS", "false"));
        if (allowHeaders) {
            registry.addInterceptor(brandContextInterceptor)
                    .addPathPatterns("/**");
        }
    }
}
