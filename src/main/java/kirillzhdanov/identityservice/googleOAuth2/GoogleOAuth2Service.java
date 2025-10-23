package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.ProvisioningServiceOps;
import kirillzhdanov.identityservice.service.TokenService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOAuth2Service {

    public record Tokens(String accessToken, String refreshToken) {}

    private final JwtUtils jwtUtils;
    private final TokenService tokenService;
    private final ProvisioningServiceOps provisioningService;
    private final OAuth2UserLinker linker;

    public GoogleOAuth2Service(JwtUtils jwtUtils,
                               TokenService tokenService,
                               ProvisioningServiceOps provisioningService,
                               OAuth2UserLinker linker) {
        this.jwtUtils = jwtUtils;
        this.tokenService = tokenService;
        this.provisioningService = provisioningService;
        this.linker = linker;
    }

    /**
     * Creates or finds local user for Google OIDC user and issues JWT tokens.
     */
    @Transactional
    public Tokens handleLoginOrRegister(OidcUser oidcUser) {
        OAuth2UserLinker.Result userRes = linker.linkOrCreate(oidcUser);
        User user = userRes.user();

        // Provision only on first Google sign-in (when we actually created a new local user)
        if (userRes.created()) {
            MasterAccount master = provisioningService.ensureMasterAccountForUser(user);
            provisioningService.ensureOwnerMembership(user, master);
            // Автоматическое создание бренда отключено: пользователь создаст бренд вручную позже
        }

        // Revoke existing access tokens optionally, keep refresh strategy if needed
        // tokenService.revokeAllUserTokens(user);

        CustomUserDetails cud = new CustomUserDetails(user);
        String access = jwtUtils.generateAccessToken(cud);
        String refresh = jwtUtils.generateRefreshToken(cud);

        tokenService.saveToken(access, Token.TokenType.ACCESS, user);
        tokenService.saveToken(refresh, Token.TokenType.REFRESH, user);

        return new Tokens(access, refresh);
    }

    
}
