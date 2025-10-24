package kirillzhdanov.identityservice.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.service.ProvisioningServiceOps;
import kirillzhdanov.identityservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционный тест, жёстко проверяющий сценарий OAuth2-пользователя:
 * - бренды видны в /auth/v1/brands даже если ctx не выбран (fallback master);
 * - после /auth/v1/context/switch устанавливается HttpOnly cookie ctx и защищённые ручки перестают отдавать 401.
 */
public class OAuth2BrandsContextIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

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
    @Autowired
    private ProvisioningServiceOps provisioning;

    private String username;
    private String rawPassword;

    @BeforeEach
    void init() {
        username = "oauth2_like_user";
        rawPassword = "pass";
    }

    @Test
    @DisplayName("OAuth2-like: brands visible without ctx; ctx cookie set on switch; protected endpoint 200")
    void oauth2BrandsAndCtxFlow() throws Exception {
        // given: пользователь без брендов в user.brands, затем выполняем провизию как при первой регистрации
        User u = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .email("oauth2.like@example.com")
                .emailVerified(true)
                .build();
        u = userService.save(u);

        // Провизия (как при первом входе через OAuth2)
        MasterAccount master = provisioning.ensureMasterAccountForUser(u);
        provisioning.ensureOwnerMembership(u, master);
        provisioning.ensureDefaultBrandAndPickup(u, master);

        // sanity: есть хотя бы один membership с brand
        var memberships = userMembershipRepository.findByUserId(u.getId());
        assertThat(memberships).anyMatch(m -> m.getBrand() != null && "ACTIVE".equalsIgnoreCase(m.getStatus()));

        // when: обычный login (как на фронте) — Basic Authorization header, тело пустое
        String basic = "Basic " + Base64.getEncoder().encodeToString((username + ":" + rawPassword).getBytes(StandardCharsets.UTF_8));
        MvcResult loginRes = mvc.perform(post("/auth/v1/login")
                        .header("Authorization", basic))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        String accessToken = mapper.readTree(loginRes.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("accessToken").asText();

        // then: без ctx-cookie запрос брендов не пустой (fallback master в BrandService)
        MvcResult brandsRes = mvc.perform(get("/auth/v1/brands")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode brands = mapper.readTree(brandsRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(brands.isArray()).isTrue();
        assertThat(brands.size()).isGreaterThanOrEqualTo(1);

        // and: получаем memberships для выбора контекста
        MvcResult memRes = mvc.perform(get("/auth/v1/memberships")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode memList = mapper.readTree(memRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(memList.isArray()).isTrue();
        JsonNode mem = null;
        for (JsonNode m : memList) {
            if (m.hasNonNull("brandId")) {
                mem = m;
                break;
            }
        }
        assertThat(mem).as("Must have at least one membership with brandId").isNotNull();
        Long membershipId = mem.get("membershipId").asLong();
        Long brandId = mem.get("brandId").asLong();
        // brandId from membership should be present in brands list
        boolean brandListed = false;
        for (JsonNode b : brands) {
            if (b.hasNonNull("id") && b.get("id").asLong() == brandId) {
                brandListed = true;
                break;
            }
        }
        assertThat(brandListed).as("brandId=" + brandId + " must be listed in /brands").isTrue();

        // before switch: protected endpoint should return 403 (authenticated but missing tenant context)
        mvc.perform(get("/auth/v1/products/by-brand/" + brandId)
                        .param("visibleOnly", "false")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        // when: switch context -> должен прийти новый токен и установиться HttpOnly cookie ctx
        String payload = "{\"membershipId\":" + membershipId + (brandId != null ? ",\"brandId\":" + brandId : "") + "}";
        MvcResult switchRes = mvc.perform(post("/auth/v1/context/switch")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("ctx=")))
                .andReturn();
        String newAccessToken = mapper.readTree(switchRes.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("accessToken").asText();
        String setCookie = switchRes.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotBlank();
        // извлекаем значение cookie ctx
        String ctxCookieValue = extractCookieValue(setCookie, "ctx");
        assertThat(ctxCookieValue).isNotBlank();
        // cookie attributes should be set
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("SameSite=Lax");

        // then: защищённый эндпоинт больше не отдаёт 401 при наличии ctx-cookie
        mvc.perform(get("/auth/v1/products/by-brand/" + brandId)
                        .param("visibleOnly", "false")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .header("Cookie", "ctx=" + ctxCookieValue))
                .andExpect(status().isOk());
    }

    private static String extractCookieValue(String setCookieHeader, String name) {
        if (setCookieHeader == null) return null;
        // Формат: ctx=<value>; Path=/; HttpOnly; ... — извлекаем между 'name=' и ';'
        int idx = setCookieHeader.indexOf(name + "=");
        if (idx < 0) return null;
        int start = idx + name.length() + 1;
        int semi = setCookieHeader.indexOf(';', start);
        if (semi < 0) semi = setCookieHeader.length();
        return setCookieHeader.substring(start, semi);
    }

    @Test
    @DisplayName("Standard registration: brands visible without ctx; ctx cookie set on switch; protected endpoint 200")
    void standardRegistrationFlow() throws Exception {
        // when: регистрируем нового пользователя через REST
        String regUsername = "reg_user_" + System.currentTimeMillis();
        String payload = "{\"username\":\"" + regUsername + "\",\"password\":\"pass\",\"email\":\"reg.it@example.com\"}";
        MvcResult regRes = mvc.perform(post("/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        String accessToken = mapper.readTree(regRes.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("accessToken").asText();

        // then: бренды доступны без выбранного ctx (fallback master)
        MvcResult brandsRes = mvc.perform(get("/auth/v1/brands")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode brands = mapper.readTree(brandsRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(brands.isArray()).isTrue();
        assertThat(brands.size()).isGreaterThanOrEqualTo(1);

        // and: получаем memberships и выбираем контекст, ожидаем установку ctx cookie
        MvcResult memRes = mvc.perform(get("/auth/v1/memberships")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode memList = mapper.readTree(memRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(memList.isArray()).isTrue();
        JsonNode mem = null;
        for (JsonNode m : memList) {
            if (m.hasNonNull("brandId")) {
                mem = m;
                break;
            }
        }
        assertThat(mem).as("Must have at least one membership with brandId").isNotNull();
        Long membershipId = mem.get("membershipId").asLong();
        Long brandId = mem.get("brandId").asLong();
        // brandId from membership should be present in brands list
        boolean brandListed = false;
        for (JsonNode b : brands) {
            if (b.hasNonNull("id") && b.get("id").asLong() == brandId) {
                brandListed = true;
                break;
            }
        }
        assertThat(brandListed).as("brandId=" + brandId + " must be listed in /brands").isTrue();

        // before switch: protected endpoint should return 403 (authenticated but missing tenant context)
        mvc.perform(get("/auth/v1/products/by-brand/" + brandId)
                        .param("visibleOnly", "false")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        String switchPayload = "{\"membershipId\":" + membershipId + (brandId != null ? ",\"brandId\":" + brandId : "") + "}";
        MvcResult switchRes = mvc.perform(post("/auth/v1/context/switch")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(switchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("ctx=")))
                .andReturn();
        String newAccessToken = mapper.readTree(switchRes.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("accessToken").asText();
        String setCookie = switchRes.getResponse().getHeader("Set-Cookie");
        String ctxCookieValue = extractCookieValue(setCookie, "ctx");
        assertThat(ctxCookieValue).isNotBlank();
        // cookie attributes should be set
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("SameSite=Lax");

        // then: защищённый эндпоинт отвечает 200 при наличии ctx-cookie
        mvc.perform(get("/auth/v1/products/by-brand/" + brandId)
                        .param("visibleOnly", "false")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .header("Cookie", "ctx=" + ctxCookieValue))
                .andExpect(status().isOk());
    }
}
