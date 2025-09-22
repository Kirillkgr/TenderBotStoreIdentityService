package kirillzhdanov.identityservice.googleOAuth2;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class OAuth2LoginSuccessHandlerTest {

    @Test
    @DisplayName("OAuth2SuccessHandler выставляет 2 куки и редиректит на success URL")
    void setsCookiesAndRedirects() throws ServletException, IOException {
        // Mocks
        GoogleOAuth2Service service = Mockito.mock(GoogleOAuth2Service.class);
        GoogleOAuth2Service.Tokens tokens = new GoogleOAuth2Service.Tokens("access.jwt", "refresh.jwt");
        when(service.handleLoginOrRegister(Mockito.any())).thenReturn(tokens);

        OidcUser oidcUser = Mockito.mock(OidcUser.class);
        when(oidcUser.getSubject()).thenReturn("sub-123");
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        when(oidcUser.getPicture()).thenReturn("https://example.com/pic.jpg");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(service);
        // Inject @Value fields
        ReflectionTestUtils.setField(handler, "successRedirectUrl", "/");
        ReflectionTestUtils.setField(handler, "cookieDomain", "");
        ReflectionTestUtils.setField(handler, "accessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(handler, "refreshExpirationMs", 2592000000L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Execute
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert Set-Cookie headers present for both tokens
        String setCookieHeader = String.join("\n", response.getHeaders("Set-Cookie"));
        assertThat(setCookieHeader).contains("accessToken=access.jwt");
        assertThat(setCookieHeader).contains("refreshToken=refresh.jwt");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Secure");
        assertThat(setCookieHeader).contains("Path=/");
        assertThat(setCookieHeader).contains("SameSite=None");

        // Assert redirect
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }
}
