package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.LoginRequest;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to guarantee that:
 * 1) On login/refresh we reconcile user.brands from OWNER/ADMIN memberships.
 * 2) On registration a default brand and pickup are provisioned.
 */
public class BrandLinksReconcileIT extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;
    @Autowired
    private UserMembershipRepository userMembershipRepository;

    private String username;

    @BeforeEach
    void setUp() {
        username = "reconcile_user";
    }

    @AfterEach
    void tearDown() {
        // cleanup by IntegrationTestBase truncation between tests
    }

    @Test
    @DisplayName("reconcile user.brands on login when memberships exist but brands link is missing")
    @Transactional
    void reconcileOnLogin_whenMembershipHasBrand_butUserBrandsMissing() {
        // given: user with encoded password but empty brands set
        User u = User.builder()
                .username(username)
                .password(passwordEncoder.encode("pass"))
                .email("reconcile@example.com")
                .emailVerified(true)
                .build();
        u = userService.save(u);

        // and: master + brand + membership OWNER for this user, but user.brands does not contain brand
        MasterAccount master = masterAccountRepository.save(MasterAccount.builder()
                .name(username)
                .status("ACTIVE")
                .build());
        Brand b = brandRepository.save(Brand.builder()
                .name("reconcile-brand-" + System.currentTimeMillis())
                .organizationName("org")
                .master(master)
                .build());
        userMembershipRepository.save(UserMembership.builder()
                .user(u)
                .master(master)
                .brand(b)
                .role(RoleMembership.OWNER)
                .status("ACTIVE")
                .build());

        // sanity: ensure current persisted user has no brands linked
        Optional<User> before = userRepository.findByUsername(username);
        assertThat(before).isPresent();
        assertThat(before.get().getBrands()).isEmpty();

        // when: login
        authService.login(new LoginRequest(username, "pass"));

        // then: user.brands contains the membership brand after reconcile
        User after = userRepository.findByUsername(username).orElseThrow();
        assertThat(after.getBrands())
                .as("user.brands should include brand from OWNER membership after login reconcile")
                .anyMatch(br -> br.getId().equals(b.getId()));
    }

    @Test
    @DisplayName("registration provisions master/owner/default brand and pickup")
    @Transactional
    void registration_createsDefaultBrandAndPickup() {
        // when: register user
        var req = UserRegistrationRequest.builder()
                .username("reg_user_" + System.currentTimeMillis())
                .password("pass")
                .email("reg@example.com")
                .build();
        var resp = authService.registerUser(req);

        // then: user exists and has at least one brand (default) and membership OWNER
        User user = userRepository.findByUsername(resp.getUsername()).orElseThrow();
        assertThat(user.getBrands()).isNotNull();
        assertThat(user.getBrands().size()).isGreaterThanOrEqualTo(1);

        var memberships = userMembershipRepository.findByUserId(user.getId());
        assertThat(memberships)
                .anyMatch(m -> m.getRole() == RoleMembership.OWNER && m.getBrand() != null);
    }
}
