package kirillzhdanov.identityservice.googleOAuth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Minimal failure handler: redirects to the login page with an error marker.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.front.failure-redirect:/login?oauth2=error}")
    private String failureRedirectUrl;

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 failure: {}", exception.getMessage());
        response.sendRedirect(failureRedirectUrl);
    }
}
