package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.UserProvider;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.UserProviderRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
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
    private final MasterAccountRepository masterAccountRepository;

    public GoogleOAuth2Service(UserRepository userRepository,
                               RoleService roleService,
                               JwtUtils jwtUtils,
                               TokenService tokenService,
                               UserProviderRepository userProviderRepository,
                               MasterAccountRepository masterAccountRepository) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.jwtUtils = jwtUtils;
        this.tokenService = tokenService;
        this.userProviderRepository = userProviderRepository;
        this.masterAccountRepository = masterAccountRepository;
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
        // Fallback: если Google не отдал given/family, попробуем разобрать полное имя
        if ((firstName == null || firstName.isBlank()) && oidcUser.getFullName() != null) {
            String full = oidcUser.getFullName();
            if (!full.isBlank()) {
                String[] parts = full.trim().split("\\s+");
                if (parts.length >= 1) firstName = parts[0];
                if (parts.length >= 2) lastName = parts[parts.length - 1];
            }
        }
        String picture = oidcUser.getPicture(); // URL аватарки Google
        Boolean emailVerified = oidcUser.getEmailVerified();

        User user = findOrCreateAndLinkUser(email, sub, firstName, lastName, picture, emailVerified);

        // Ensure MasterAccount exists for this user (same behavior as classic registration)
        try {
            String username = user.getUsername();
            masterAccountRepository.findByName(username)
                    .orElseGet(() -> masterAccountRepository.save(MasterAccount.builder()
                            .name(username)
                            .status("ACTIVE")
                            .build()));
        } catch (Exception ignored) {
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

    private User findOrCreateAndLinkUser(String email, String sub, String firstName, String lastName, String pictureUrl, Boolean emailVerified) {
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
            // Не перезаписываем аватарку из Google, если пользователь уже существует.
            // Дополняем только недостающие ФИО и флаг подтверждения почты.
            if ((user.getFirstName() == null || user.getFirstName().isBlank()) && firstName != null && !firstName.isBlank()) {
                user.setFirstName(firstName);
            }
            if ((user.getLastName() == null || user.getLastName().isBlank()) && lastName != null && !lastName.isBlank()) {
                user.setLastName(lastName);
            }
            if (emailVerified != null && emailVerified && !user.isEmailVerified()) {
                user.setEmailVerified(true);
            }
            user = userRepository.save(user);
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
                    .avatarUrl(pictureUrl) // только при первичном создании пользователя
                    .emailVerified(emailVerified != null ? emailVerified : false)
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
