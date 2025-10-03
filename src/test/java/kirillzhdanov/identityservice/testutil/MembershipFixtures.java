package kirillzhdanov.identityservice.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.dto.ContextSwitchRequest;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тестовые фикстуры для memberships по всем ролям.
 * <p>
 * Использование (в тесте):
 *
 * @Autowired MembershipFixtures fx;
 * Cookie login = fx.registerAndLogin("user-x");
 * Map<RoleMembership, Context> contexts = fx.prepareAllRoleMemberships(login, "user-x");
 * Cookie adminCtx = contexts.get(RoleMembership.ADMIN).cookie();
 */
@Component
@Profile("dev")
public class MembershipFixtures {

    private final Map<String, Cookie> userTokenCache = new ConcurrentHashMap<>();
    @Autowired(required = false)
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private MasterAccountRepository masterRepo;
    @Autowired
    private UserMembershipRepository membershipRepo;

    public Cookie registerAndLogin(String username) throws Exception {
        requireMockMvc();
        // Регистрация нового пользователя и первичный вход
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername(username);
        req.setEmail(username + "@test.local");
        req.setPassword("Password123!");
        Set<Role.RoleName> roles = new HashSet<>();
        roles.add(Role.RoleName.USER);
        req.setRoleNames(roles);
        MvcResult res = mockMvc
                .perform(post("/auth/v1/register")
                        .header("X-Brand-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        UserResponse created = objectMapper.readValue(res.getResponse().getContentAsString(), UserResponse.class);
        assertThat(created.getAccessToken()).isNotBlank();
        Cookie cookie = new Cookie("accessToken", created.getAccessToken());
        userTokenCache.put(username, cookie);
        return cookie;
    }

    /**
     * Явный логин существующего пользователя.
     */
    public Cookie login(String username, String password) throws Exception {
        requireMockMvc();
        String basic = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MvcResult res = mockMvc.perform(post("/auth/v1/login")
                        .header("X-Brand-Id", "1")
                        .header("Authorization", basic))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("accessToken").asText();
        Cookie cookie = new Cookie("accessToken", accessToken);
        userTokenCache.put(username, cookie);
        return cookie;
    }

    /**
     * Обеспечивает наличие токена для пользователя: пытается взять из кеша, затем логин; при неудаче — регистрация.
     */
    public Cookie ensureLogin(String username) throws Exception {
        Cookie cached = userTokenCache.get(username);
        if (cached != null) return cached;
        try {
            Cookie fresh = login(username, "Password123!");
            userTokenCache.put(username, fresh);
            return fresh;
        } catch (AssertionError | Exception ex) {
            return registerAndLogin(username);
        }
    }

    public Cookie switchContext(Cookie authCookie, Long membershipId) throws Exception {
        requireMockMvc();
        ContextSwitchRequest req = new ContextSwitchRequest();
        req.setMembershipId(membershipId);
        MvcResult sw = mockMvc.perform(post("/auth/v1/context/switch")
                        .header("X-Brand-Id", "1")
                        .cookie(authCookie)
                        .header("Authorization", "Bearer " + authCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(sw.getResponse().getContentAsString()).get("accessToken").asText();
        return new Cookie("accessToken", accessToken);
    }

    private Long createMembership(Long userId, MasterAccount m, RoleMembership role) {
        UserMembership um = new UserMembership();
        um.setUser(userRepo.findById(userId).orElseThrow());
        um.setMaster(m);
        um.setRole(role);
        return membershipRepo.save(um).getId();
    }

    /**
     * Создаёт по одному membership на каждого из RoleMembership для указанного пользователя.
     * Для избежания конфликтов уникальности создаём отдельный Master на каждую роль.
     * Возвращает карту role -> Context (membershipId, masterId, cookie после switchContext).
     */
    public Map<RoleMembership, Context> prepareAllRoleMemberships(Cookie loginCookie, String username) throws Exception {
        requireMockMvc();
        Long userId = Objects.requireNonNull(userRepo.findByUsername(username)
                        .orElseGet(() -> {
                            try {
                                registerAndLogin(username);
                            } catch (Exception ignored) {
                            }
                            return userRepo.findByUsername(username).orElse(null);
                        }))
                .getId();
        Map<RoleMembership, Context> map = new EnumMap<>(RoleMembership.class);
        for (RoleMembership role : RoleMembership.values()) {
            MasterAccount m = masterRepo.save(MasterAccount.builder().name("FX-" + role.name()).status("ACTIVE").build());
            Long memId = createMembership(userId, m, role);
            Cookie ctx = switchContext(loginCookie, memId);
            map.put(role, new Context(memId, m.getId(), ctx));
        }
        return map;
    }

    /**
     * Создаёт membership для одной роли. Создаёт новый Master автоматически.
     */
    public Context prepareRoleMembership(Cookie loginCookie, String username, RoleMembership role) throws Exception {
        requireMockMvc();
        Long userId = Objects.requireNonNull(userRepo.findByUsername(username)
                        .orElseGet(() -> {
                            try {
                                registerAndLogin(username);
                            } catch (Exception ignored) {
                            }
                            return userRepo.findByUsername(username).orElse(null);
                        }))
                .getId();
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("FX-" + role.name()).status("ACTIVE").build());
        Long memId = createMembership(userId, m, role);
        Cookie ctx = switchContext(loginCookie, memId);
        return new Context(memId, m.getId(), ctx);
    }

    /**
     * Создаёт membership для роли в указанном master (если нужен контроль бренда/мастера в тесте).
     */
    public Context prepareRoleMembershipInMaster(Cookie loginCookie, String username, RoleMembership role, MasterAccount master) throws Exception {
        requireMockMvc();
        Long userId = Objects.requireNonNull(userRepo.findByUsername(username)
                        .orElseGet(() -> {
                            try {
                                registerAndLogin(username);
                            } catch (Exception ignored) {
                            }
                            return userRepo.findByUsername(username).orElse(null);
                        }))
                .getId();
        Long memId = createMembership(userId, master, role);
        Cookie ctx = switchContext(loginCookie, memId);
        return new Context(memId, master.getId(), ctx);
    }

    private void requireMockMvc() {
        if (mockMvc == null) {
            throw new IllegalStateException("MockMvc is required for MembershipFixtures methods. Ensure @AutoConfigureMockMvc is present.");
        }
    }

    public record Context(Long membershipId, Long masterId, Cookie cookie) {
    }
}
