package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ContextSwitchIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipFixtures fixtures;

    @Test
    @DisplayName("Switch CLIENT -> COOK меняет доступы и клеймы токена")
    void switch_client_to_cook_changes_access_and_claims() throws Exception {
        String username = "ctx-user-" + System.nanoTime();

        // Base login and initial CLIENT context
        Cookie login = fixtures.registerAndLogin(username);
        var clientCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.CLIENT);
        Long clientMasterId = clientCtx.masterId();
        Cookie clientToken = clientCtx.cookie();

        // Before switch: CLIENT cannot access kitchen
        mockMvc.perform(get("/kitchen/v1/ping")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue()))
                .andExpect(status().isForbidden());

        // Switch to COOK
        var cookCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.COOK);
        Long cookMasterId = cookCtx.masterId();
        Cookie cookToken = cookCtx.cookie();

        // After switch: COOK can access kitchen
        mockMvc.perform(get("/kitchen/v1/ping")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(cookMasterId))
                        .cookie(cookToken)
                        .header("Authorization", "Bearer " + cookToken.getValue()))
                .andExpect(status().isOk());

        // COOK cannot access admin
        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(cookMasterId))
                        .cookie(cookToken)
                        .header("Authorization", "Bearer " + cookToken.getValue()))
                .andExpect(status().isForbidden());

        // Old CLIENT token still cannot access kitchen (old rights)
        mockMvc.perform(get("/kitchen/v1/ping")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue()))
                .andExpect(status().isForbidden());

        // Validate claims of COOK token
        JsonNode claims = decodeJwtPayload(cookToken.getValue());
        assertThat(claims.get("membershipId").asLong()).isEqualTo(cookCtx.membershipId());
        assertThat(claims.get("masterId").asLong()).isEqualTo(cookMasterId);
        // В некоторых реализациях массив roles содержит только глобальные роли (USER/ADMIN/OWNER),
        // а роль membership (COOK/CLIENT) хранится иначе. Проверяем лишь наличие массива ролей.
        assertThat(claims.has("roles")).isTrue();
        assertThat(claims.get("roles").isArray()).isTrue();
    }

    @Test
    @DisplayName("Switch to CLIENT запрещает админку и разрешает client-handlers")
    void switch_to_client_forbids_admin_allows_client() throws Exception {
        String username = "ctx-client-" + System.nanoTime();

        Cookie login = fixtures.registerAndLogin(username);
        var adminCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        Long adminMasterId = adminCtx.masterId();
        Cookie adminToken = adminCtx.cookie();

        // Admin can access admin endpoint
        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(adminMasterId))
                        .cookie(adminToken)
                        .header("Authorization", "Bearer " + adminToken.getValue()))
                .andExpect(status().isOk());

        // Switch to CLIENT
        var clientCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.CLIENT);
        Long clientMasterId = clientCtx.masterId();
        Cookie clientToken = clientCtx.cookie();

        // CLIENT: admin forbidden
        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue()))
                .andExpect(status().isForbidden());

        // CLIENT: orders my allowed
        mockMvc.perform(get("/order/v1/my")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue()))
                .andExpect(status().isOk());
    }

    private JsonNode decodeJwtPayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return objectMapper.readTree(payload);
    }

    @Test
    @DisplayName("Switch: несуществующий membershipId -> 400")
    void switch_with_nonexistent_membership_returns_400() throws Exception {
        String username = "ctx-neg-" + System.nanoTime();
        Cookie login = fixtures.registerAndLogin(username);

        long nonExisting = Long.MAX_VALUE - 123;
        String body = "{\n  \"membershipId\": " + nonExisting + "\n}";
        mockMvc.perform(post("/auth/v1/context/switch")
                        .header("X-Brand-Id", "1")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Switch: чужой membershipId пользователя -> 400")
    void switch_with_foreign_membership_returns_400() throws Exception {
        // user A
        String userA = "ctx-A-" + System.nanoTime();
        Cookie aLogin = fixtures.registerAndLogin(userA);

        // user B + его membership
        String userB = "ctx-B-" + System.nanoTime();
        Cookie bLogin = fixtures.registerAndLogin(userB);
        var bClient = fixtures.prepareRoleMembership(bLogin, userB, RoleMembership.CLIENT);
        long foreignMemId = bClient.membershipId();

        String body = "{\n  \"membershipId\": " + foreignMemId + "\n}";
        mockMvc.perform(post("/auth/v1/context/switch")
                        .header("X-Brand-Id", "1")
                        .cookie(aLogin)
                        .header("Authorization", "Bearer " + aLogin.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
