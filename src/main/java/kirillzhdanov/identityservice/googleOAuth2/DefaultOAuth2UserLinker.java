package kirillzhdanov.identityservice.googleOAuth2;

import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.UserProvider;
import kirillzhdanov.identityservice.repository.UserProviderRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.service.RoleService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Component
public class DefaultOAuth2UserLinker implements OAuth2UserLinker {

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final RoleService roleService;

    public DefaultOAuth2UserLinker(UserRepository userRepository,
                                   UserProviderRepository userProviderRepository,
                                   RoleService roleService) {
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
        this.roleService = roleService;
    }

    @Override
    @Transactional
    public Result linkOrCreate(OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        String sub = oidcUser.getSubject();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();
        if ((firstName == null || firstName.isBlank()) && oidcUser.getFullName() != null) {
            String full = oidcUser.getFullName();
            if (!full.isBlank()) {
                String[] parts = full.trim().split("\\s+");
                if (parts.length >= 1) firstName = parts[0];
                if (parts.length >= 2) lastName = parts[parts.length - 1];
            }
        }
        String picture = oidcUser.getPicture();
        Boolean emailVerified = oidcUser.getEmailVerified();

        Optional<UserProvider> mapped = userProviderRepository
                .findByProviderAndProviderUserId(UserProvider.Provider.GOOGLE, sub);
        if (mapped.isPresent()) {
            return new Result(mapped.get().getUser(), false);
        }

        Optional<User> byEmail = Optional.empty();
        if (email != null && !email.isBlank()) {
            byEmail = userRepository.findByEmail(email);
        }

        User user;
        boolean created = false;
        if (byEmail.isPresent()) {
            user = byEmail.get();
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
            String usernameBase = (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : "google_" + sub;
            String username = ensureUniqueUsername(usernameBase);

            user = User.builder()
                    .username(username)
                    .password("")
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .avatarUrl(picture)
                    .emailVerified(emailVerified != null ? emailVerified : false)
                    .roles(new HashSet<>())
                    .brands(new HashSet<>())
                    .createdAt(LocalDateTime.now())
                    .build();

            Role userRole = roleService.getUserRole();
            user.getRoles().add(userRole);
            user = userRepository.save(user);
            created = true;
        }

        if (!userProviderRepository.existsByProviderAndProviderUserId(UserProvider.Provider.GOOGLE, sub)) {
            UserProvider link = UserProvider.builder()
                    .user(user)
                    .provider(UserProvider.Provider.GOOGLE)
                    .providerUserId(sub)
                    .build();
            userProviderRepository.save(link);
        }
        return new Result(user, created);
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
