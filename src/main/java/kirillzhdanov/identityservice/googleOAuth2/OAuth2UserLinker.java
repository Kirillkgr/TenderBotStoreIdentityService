package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface OAuth2UserLinker {
    record Result(User user, boolean created) {}
    Result linkOrCreate(OidcUser oidcUser);
}
