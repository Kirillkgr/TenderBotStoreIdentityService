package kirillzhdanov.identityservice.security;

import kirillzhdanov.identityservice.googleOAuth2.CustomOidcUserService;
import kirillzhdanov.identityservice.tenant.ContextEnforcementFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOidcUserService oidcUserService = new CustomOidcUserService();
    private final kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final Environment environment;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginSuccessHandler successHandler,
                          kirillzhdanov.identityservice.googleOAuth2.OAuth2LoginFailureHandler failureHandler,
                          Environment environment) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2LoginSuccessHandler = successHandler;
        this.oAuth2LoginFailureHandler = failureHandler;
        this.environment = environment;
    }

    @Bean
    public OncePerRequestFilter tenantContextFilter() {
        return new kirillzhdanov.identityservice.tenant.TenantContextFilter();
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Add dev-only header-based tenant context filter
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            http.addFilterAfter(tenantContextFilter(), UsernamePasswordAuthenticationFilter.class);
            http.addFilterAfter(contextEnforcementFilter(), tenantContextFilter().getClass());
        } else {
            // Without dev filter, enforce right after JWT auth
            http.addFilterAfter(contextEnforcementFilter(), UsernamePasswordAuthenticationFilter.class);
        }

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
