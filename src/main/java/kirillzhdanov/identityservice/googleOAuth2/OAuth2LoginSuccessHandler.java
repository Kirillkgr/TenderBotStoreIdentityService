package kirillzhdanov.identityservice.googleOAuth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

    public OAuth2LoginSuccessHandler(GoogleOAuth2Service googleOAuth2Service) {
        this.googleOAuth2Service = googleOAuth2Service;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        log.info("OAuth2 success for provider=GOOGLE, sub={}, email={}", oidcUser.getSubject(), oidcUser.getEmail());

        GoogleOAuth2Service.Tokens tokens = googleOAuth2Service.handleLoginOrRegister(oidcUser);

        // Set HttpOnly cookies for frontend to pick up via APIs
        addCookie(response, "accessToken", tokens.accessToken(), true, "/", true, "Lax");
        addCookie(response, "refreshToken", tokens.refreshToken(), true, "/", true, "Lax");

        response.sendRedirect(successRedirectUrl);
    }

    private void addCookie(HttpServletResponse response, String name, String value, boolean httpOnly,
                           String path, boolean secure, String sameSite) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath(path);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookie.setDomain(cookieDomain);
        }
        // SameSite not in standard Cookie API for Java 21; set via header
        response.addHeader("Set-Cookie", String.format("%s=%s; Path=%s; %s; %s%s%s",
                name, value, path,
                secure ? "Secure" : "",
                httpOnly ? "HttpOnly" : "",
                (cookieDomain != null && !cookieDomain.isBlank()) ? "; Domain=" + cookieDomain : "",
                "; SameSite=" + sameSite));
    }
}
