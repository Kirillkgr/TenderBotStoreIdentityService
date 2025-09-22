package kirillzhdanov.identityservice.googleOAuth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleOAuth2Service googleOAuth2Service;
    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Value("${app.front.success-redirect:/}")
    private String successRedirectUrl;

    @Value("${server.servlet.session.cookie.domain:}")
    private String cookieDomain;

    @Value("${jwt.access.expiration:3600000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh.expiration:2592000000}")
    private long refreshExpirationMs;

    public OAuth2LoginSuccessHandler(GoogleOAuth2Service googleOAuth2Service) {
        this.googleOAuth2Service = googleOAuth2Service;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        log.info("OAuth2 success for provider=GOOGLE, sub={}, email={}", oidcUser.getSubject(), oidcUser.getEmail());
        // Диагностика запроса
        String host = request.getHeader("Host");
        String xfProto = request.getHeader("X-Forwarded-Proto");
        String xfFor = request.getHeader("X-Forwarded-For");
        log.info("OAuth2 callback request: url={}{} host={} xfp={} xff={}",
                request.getRequestURL(),
                request.getQueryString() != null ? ("?" + request.getQueryString()) : "",
                host, xfProto, xfFor);

        GoogleOAuth2Service.Tokens tokens = googleOAuth2Service.handleLoginOrRegister(oidcUser);

        // Унификация с базовой авторизацией:
        // кладём только refreshToken в HttpOnly cookie.
        // accessToken фронт получит отдельным вызовом /auth/v1/refresh после редиректа
        log.info("Will set refreshToken cookie for domain='{}' sameSite=None maxAgeMs={}",
                (cookieDomain == null || cookieDomain.isBlank()) ? "<default>" : cookieDomain,
                refreshExpirationMs);
        addCookie(response, "refreshToken", tokens.refreshToken(), true, "/", true, "None", refreshExpirationMs);

        log.info("Redirecting to success URL: {}", successRedirectUrl);
        response.sendRedirect(successRedirectUrl);
    }

    private void addCookie(HttpServletResponse response, String name, String value, boolean httpOnly,
                           String path, boolean secure, String sameSite, long maxAgeMs) {
        // Build Set-Cookie manually to include SameSite and Max-Age
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value)
          .append("; Path=").append(path)
          .append(secure ? "; Secure" : "")
          .append(httpOnly ? "; HttpOnly" : "")
          .append("; SameSite=").append(sameSite);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            sb.append("; Domain=").append(cookieDomain);
        }
        if (maxAgeMs > 0) {
            long seconds = Math.max(1, maxAgeMs / 1000);
            sb.append("; Max-Age=").append(seconds);
        }
        String header = sb.toString();
        log.info("Setting cookie {} (domain={}, sameSite={}, maxAgeSec={})", name,
                (cookieDomain == null || cookieDomain.isBlank()) ? "<default>" : cookieDomain,
                sameSite,
                (maxAgeMs > 0 ? maxAgeMs / 1000 : -1));
        response.addHeader("Set-Cookie", header);
    }
}
