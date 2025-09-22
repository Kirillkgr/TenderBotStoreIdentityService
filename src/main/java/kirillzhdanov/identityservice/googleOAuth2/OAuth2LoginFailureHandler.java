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
        String msg = exception.getMessage();
        log.warn("OAuth2 failure: {}", msg, exception);
        // Добавим короткую метку ошибки, чтобы на фронте было видно, что именно случилось
        String marker = "oauth2=error";
        if (msg != null) {
            if (msg.contains("invalid_token_response")) marker = "oauth2=invalid_token_response";
            else if (msg.contains("invalid_grant")) marker = "oauth2=invalid_grant";
            else if (msg.contains("unauthorized_client")) marker = "oauth2=unauthorized_client";
            else if (msg.contains("redirect_uri_mismatch")) marker = "oauth2=redirect_uri_mismatch";
        }
        String sep = failureRedirectUrl.contains("?") ? "&" : "?";
        response.sendRedirect(failureRedirectUrl + sep + marker);
    }
}
