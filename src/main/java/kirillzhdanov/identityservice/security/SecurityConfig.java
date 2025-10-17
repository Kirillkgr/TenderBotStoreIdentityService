package kirillzhdanov.identityservice.security;

import kirillzhdanov.identityservice.googleOAuth2.CustomOidcUserService;
import kirillzhdanov.identityservice.tenant.ContextEnforcementFilter;
import kirillzhdanov.identityservice.tenant.CtxCookieFilter;
import kirillzhdanov.identityservice.tenant.TenantContextCleanupFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация безопасности.
 * <p>
 * Порядок фильтров:
 * - {@link kirillzhdanov.identityservice.tenant.CtxCookieFilter} добавляется ДО {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}
 * и наполняет request-скоуп контекстом из HttpOnly cookie (brandId, pickupPointId и т.д.).
 * - JWT-аутентификация также запускается ДО UsernamePasswordAuthenticationFilter.
 * - {@link kirillzhdanov.identityservice.tenant.ContextEnforcementFilter} запускается ПОСЛЕ UsernamePasswordAuthenticationFilter
 * и проверяет наличие контекста для защищённых ручек.
 * <p>
 * Legacy {@code TenantContextFilter} удалён из цепочки (и из проекта), заголовки X-* больше не используются.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOidcUserService oidcUserService = new CustomOidcUserService();
    private final kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final CtxCookieFilter ctxCookieFilter;
    private final TenantContextCleanupFilter tenantContextCleanupFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginSuccessHandler successHandler,
                          kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginFailureHandler failureHandler,
                          CtxCookieFilter ctxCookieFilter,
                          TenantContextCleanupFilter tenantContextCleanupFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2LoginSuccessHandler = successHandler;
        this.oAuth2LoginFailureHandler = failureHandler;
        this.ctxCookieFilter = ctxCookieFilter;
        this.tenantContextCleanupFilter = tenantContextCleanupFilter;
    }

    @Bean
    public ContextEnforcementFilter contextEnforcementFilter() {
        return new ContextEnforcementFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight
                        // Long-poll: GET /notifications/longpoll доступен всем (контроллер вернёт 204 для неавторизованных)
                        .requestMatchers(HttpMethod.GET, "/notifications/longpoll").permitAll()
                        // ACK и unreadCount доступны только авторизованным
                        .requestMatchers("/notifications/longpoll/ack", "/notifications/longpoll/unreadCount").authenticated()
                        .requestMatchers("/auth/v1/login")
                        .permitAll()
                        .requestMatchers("/auth/v1/register")
                        .permitAll()
                        .requestMatchers("/auth/v1/checkUsername")
                        .permitAll()
                        .requestMatchers("/auth/v1/refresh")
                        .permitAll()
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/menu/v1/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/menu/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/public/v1/**").permitAll()
                        .requestMatchers("/cart/**").permitAll()

                        .requestMatchers("/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/api-docs/**")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/status")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // Important for REST: return 401/403 instead of redirecting to login page
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> response.sendError(401))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(403))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Очистка legacy TenantContext — строго первой
                .addFilterBefore(tenantContextCleanupFilter, UsernamePasswordAuthenticationFilter.class)
                // ctx cookie filter должен идти до JWT, чтобы заполнить контекст запроса
                .addFilterBefore(ctxCookieFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Enforce presence of tenant context. Anchor after UsernamePasswordAuthenticationFilter to avoid
        // referencing a custom filter class without a registered order.
        http.addFilterAfter(contextEnforcementFilter(), UsernamePasswordAuthenticationFilter.class);

        http
                .oauth2Login(o -> o
                        .userInfoEndpoint(u -> u.oidcUserService(oidcUserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {

		return authenticationConfiguration.getAuthenticationManager();
	}
}
