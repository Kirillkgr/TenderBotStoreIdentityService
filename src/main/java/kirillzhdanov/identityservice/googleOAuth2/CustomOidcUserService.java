package kirillzhdanov.identityservice.googleOAuth2;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Stub for mapping Google OIDC user to local account.
 * For now returns default OidcUser; later we can link to local user record.
 */
public class CustomOidcUserService extends OidcUserService {
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        // You can map claims here and lookup your local user
        return super.loadUser(userRequest);
    }
}
