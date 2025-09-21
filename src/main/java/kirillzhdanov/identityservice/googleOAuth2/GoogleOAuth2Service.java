package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.UserProvider;
import kirillzhdanov.identityservice.repository.UserProviderRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.RoleService;
import kirillzhdanov.identityservice.service.TokenService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleOAuth2Service {

    public record Tokens(String accessToken, String refreshToken) {}

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final JwtUtils jwtUtils;
    private final TokenService tokenService;
    private final UserProviderRepository userProviderRepository;

    public GoogleOAuth2Service(UserRepository userRepository,
                               RoleService roleService,
                               JwtUtils jwtUtils,
                               TokenService tokenService,
                               UserProviderRepository userProviderRepository) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.jwtUtils = jwtUtils;
        this.tokenService = tokenService;
        this.userProviderRepository = userProviderRepository;
    }

    /**
     * Creates or finds local user for Google OIDC user and issues JWT tokens.
     */
    @Transactional
    public Tokens handleLoginOrRegister(OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        String sub = oidcUser.getSubject();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();

        User user = findOrCreateAndLinkUser(email, sub, firstName, lastName);

        // Revoke existing access tokens optionally, keep refresh strategy if needed
        // tokenService.revokeAllUserTokens(user);

        CustomUserDetails cud = new CustomUserDetails(user);
        String access = jwtUtils.generateAccessToken(cud);
        String refresh = jwtUtils.generateRefreshToken(cud);

        tokenService.saveToken(access, Token.TokenType.ACCESS, user);
        tokenService.saveToken(refresh, Token.TokenType.REFRESH, user);

        return new Tokens(access, refresh);
    }

    private User findOrCreateAndLinkUser(String email, String sub, String firstName, String lastName) {
        // 1) Check link by provider+sub
        Optional<UserProvider> mapped = userProviderRepository.findByProviderAndProviderUserId(UserProvider.Provider.GOOGLE, sub);
        if (mapped.isPresent()) {
            return mapped.get().getUser();
        }

        // 2) If email exists, link to that user
        Optional<User> byEmail = Optional.empty();
        if (email != null && !email.isBlank()) {
            byEmail = userRepository.findByEmail(email);
        }
        User user;
        if (byEmail.isPresent()) {
            user = byEmail.get();
        } else {
            // 3) Create a new user
            String usernameBase = (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : "google_" + sub;
            String username = ensureUniqueUsername(usernameBase);

            user = User.builder()
                    .username(username)
                    .password("")
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .emailVerified(true)
                    .roles(new HashSet<>())
                    .brands(new HashSet<>())
                    .createdAt(LocalDateTime.now())
                    .build();

            Role userRole = roleService.getUserRole();
            user.getRoles().add(userRole);
            user = userRepository.save(user);
        }

        // 4) Create mapping user_providers
        if (!userProviderRepository.existsByProviderAndProviderUserId(UserProvider.Provider.GOOGLE, sub)) {
            UserProvider link = UserProvider.builder()
                    .user(user)
                    .provider(UserProvider.Provider.GOOGLE)
                    .providerUserId(sub)
                    .build();
            userProviderRepository.save(link);
        }
        return user;
    }

    private String ensureUniqueUsername(String base) {
        String u = base;
        int attempt = 0;
        while (userRepository.existsByUsername(u)) {
            attempt++;
            u = base + "_" + attempt;
            if (attempt > 25) {
                u = base + "_" + UUID.randomUUID().toString().substring(0, 8);
                if (!userRepository.existsByUsername(u)) break;
            }
        }
        return u;
    }
}
