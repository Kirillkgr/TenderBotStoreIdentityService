package kirillzhdanov.identityservice.controller.cart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import kirillzhdanov.identityservice.testutil.ScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class CartMergeIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MembershipFixtures fixtures;
    @Autowired
    private ScenarioBuilder sb;

    private String adminUser;
    private Cookie adminLogin;

    @BeforeEach
    void setup() throws Exception {
        adminUser = "cart-admin-" + System.nanoTime();
        adminLogin = fixtures.ensureLogin(adminUser);
    }

    @Test
    @DisplayName("Гость добавляет в корзину -> логин с передачей cart_token -> корзина привязывается к пользователю")
    void guestCartAttachesOnLogin() throws Exception {
        // 1) Подготовим бренд/продукт админом
        var adminCtx = fixtures.prepareRoleMembership(adminLogin, adminUser, RoleMembership.ADMIN);
        long brandId = sb.createBrand(adminCtx.cookie(), adminCtx.masterId(), "CART-BRAND", "ORG");
        // Переключим контекст на этот бренд перед приватной операцией создания продукта
        var ctxReq = new kirillzhdanov.identityservice.dto.ContextSwitchRequest();
        ctxReq.setMembershipId(adminCtx.membershipId());
        ctxReq.setBrandId(brandId);
        var ctxRes = mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(adminCtx.cookie())
                        .header("Authorization", "Bearer " + adminCtx.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctxReq)))
                .andExpect(status().isOk())
                .andReturn();
        String newAccess = objectMapper.readTree(ctxRes.getResponse().getContentAsString()).get("accessToken").asText();
        Cookie ctxCookie = new Cookie("accessToken", newAccess);
        long productId = sb.createProduct(ctxCookie, adminCtx.masterId(), "CART-PROD", new BigDecimal("12.34"), brandId);

        // 2) Гость добавляет товар в корзину -> сервер установит cart_token
        String body = objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 2));
        MvcResult guestAdd = mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        List<String> setCookies = guestAdd.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotEmpty();
        // Вытащим cart_token из всех Set-Cookie
        String cartToken = null;
        outer:
        for (String cookieHdr : setCookies) {
            for (String part : cookieHdr.split(";")) {
                String p = part.trim();
                if (p.startsWith("cart_token=")) {
                    cartToken = p.substring("cart_token=".length());
                    break outer;
                }
            }
        }
        assertThat(cartToken).isNotBlank();

        // 3) Регистрируем пользователя и логинимся, передавая cookie cart_token -> корзина должна слиться и привязаться к пользователю
        String username = "cart-user-" + System.nanoTime();
        String reg = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + username + "@test.local\"," +
                "\"password\":\"Password123!\"," +
                "\"roleNames\":[\"USER\"]}";
        mockMvc.perform(post("/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reg.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated());

        // Логин через basic header и cookie cart_token
        String basic = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":Password123!").getBytes(StandardCharsets.UTF_8));
        MvcResult loginRes = mockMvc.perform(post("/auth/v1/login")
                        .header("Authorization", basic)
                        .cookie(new Cookie("cart_token", cartToken)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("accessToken").asText();

        // 4) Теперь GET /cart под авторизацией должен вернуть позиции пользователя и без cartToken в теле
        MvcResult afterLoginCart = mockMvc.perform(get("/cart")
                        .cookie(new Cookie("accessToken", accessToken))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cart = objectMapper.readTree(afterLoginCart.getResponse().getContentAsString());
        assertThat(cart.get("items").isArray()).isTrue();
        assertThat(cart.get("items").size()).isEqualTo(1);
        // cartToken для авторизованного пользователя либо отсутствует, либо null
        assertThat(!cart.has("cartToken") || cart.get("cartToken").isNull()).isTrue();
    }

    @Test
    @DisplayName("Конфликт брендов: у пользователя уже есть корзина бренда A, у гостя — бренда B -> гость очищается, остаётся пользовательская")
    void brandConflictKeepsUserCart() throws Exception {
        // 1) Подготовим 2 бренда и продукты
        var adminCtx = fixtures.prepareRoleMembership(adminLogin, adminUser, RoleMembership.ADMIN);
        long brandA = sb.createBrand(adminCtx.cookie(), adminCtx.masterId(), "BRAND-A", "ORGA");
        // ctx -> A
        var ctxReqA = new kirillzhdanov.identityservice.dto.ContextSwitchRequest();
        ctxReqA.setMembershipId(adminCtx.membershipId());
        ctxReqA.setBrandId(brandA);
        var ctxResA = mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(adminCtx.cookie())
                        .header("Authorization", "Bearer " + adminCtx.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctxReqA)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie ctxCookieA = new Cookie("accessToken", objectMapper.readTree(ctxResA.getResponse().getContentAsString()).get("accessToken").asText());
        long prodA = sb.createProduct(ctxCookieA, adminCtx.masterId(), "PA", new BigDecimal("10"), brandA);

        long brandB = sb.createBrand(adminCtx.cookie(), adminCtx.masterId(), "BRAND-B", "ORGB");
        // ctx -> B
        var ctxReqB = new kirillzhdanov.identityservice.dto.ContextSwitchRequest();
        ctxReqB.setMembershipId(adminCtx.membershipId());
        ctxReqB.setBrandId(brandB);
        var ctxResB = mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(adminCtx.cookie())
                        .header("Authorization", "Bearer " + adminCtx.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctxReqB)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie ctxCookieB = new Cookie("accessToken", objectMapper.readTree(ctxResB.getResponse().getContentAsString()).get("accessToken").asText());
        long prodB = sb.createProduct(ctxCookieB, adminCtx.masterId(), "PB", new BigDecimal("20"), brandB);

        // 2) Зарегистрируем пользователя и сформируем его корзину (бренд A)
        String username = "conflict-user-" + System.nanoTime();
        String reg = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + username + "@test.local\"," +
                "\"password\":\"Password123!\"," +
                "\"roleNames\":[\"USER\"]}";
        mockMvc.perform(post("/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reg.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated());
        String basic = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":Password123!").getBytes(StandardCharsets.UTF_8));
        MvcResult firstLogin = mockMvc.perform(post("/auth/v1/login").header("Authorization", basic))
                .andExpect(status().isOk())
                .andReturn();
        String userAccess = objectMapper.readTree(firstLogin.getResponse().getContentAsString()).get("accessToken").asText();
        // Добавим товар A в корзину пользователя (авторизованно)
        String addUserA = objectMapper.writeValueAsString(Map.of("productId", prodA, "quantity", 1));
        mockMvc.perform(post("/cart/add")
                        .cookie(new Cookie("accessToken", userAccess))
                        .header("Authorization", "Bearer " + userAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addUserA))
                .andExpect(status().isOk());

        // 3) Гость собирает корзину для бренда B
        String addGuestB = objectMapper.writeValueAsString(Map.of("productId", prodB, "quantity", 2));
        MvcResult guestAdd = mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addGuestB))
                .andExpect(status().isOk())
                .andReturn();
        List<String> setCookies2 = guestAdd.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies2).isNotEmpty();
        String cartToken = null;
        outer2:
        for (String cookieHdr : setCookies2) {
            for (String part : cookieHdr.split(";")) {
                String p = part.trim();
                if (p.startsWith("cart_token=")) {
                    cartToken = p.substring("cart_token=".length());
                    break outer2;
                }
            }
        }
        assertThat(cartToken).isNotBlank();

        // 4) Повторный логин с передачей guest cart_token => по правилам merge конфликта гость очищается
        MvcResult secondLogin = mockMvc.perform(post("/auth/v1/login")
                        .header("Authorization", basic)
                        .cookie(new Cookie("cart_token", cartToken)))
                .andExpect(status().isOk())
                .andReturn();
        String userAccess2 = objectMapper.readTree(secondLogin.getResponse().getContentAsString()).get("accessToken").asText();

        // 5) Проверяем корзину: должен остаться только товар A из пользовательской корзины
        MvcResult after = mockMvc.perform(get("/cart")
                        .cookie(new Cookie("accessToken", userAccess2))
                        .header("Authorization", "Bearer " + userAccess2))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cart = objectMapper.readTree(after.getResponse().getContentAsString());
        assertThat(cart.get("items").size()).isEqualTo(1);
        assertThat(cart.get("total").decimalValue()).isEqualByComparingTo("10");
    }
}
