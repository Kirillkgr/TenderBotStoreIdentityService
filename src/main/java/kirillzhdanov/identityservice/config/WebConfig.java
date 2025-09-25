package kirillzhdanov.identityservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BrandContextInterceptor brandContextInterceptor;

    public WebConfig(BrandContextInterceptor brandContextInterceptor) {
        this.brandContextInterceptor = brandContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(brandContextInterceptor)
                .addPathPatterns("/**");
    }
}
