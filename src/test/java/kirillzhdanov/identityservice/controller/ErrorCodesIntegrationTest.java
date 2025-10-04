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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ErrorCodesIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembershipFixtures fixtures;

    @Autowired
    private ObjectMapper objectMapper;

    // ===== 401 Unauthorized =====

    @Test
    @DisplayName("401: без токена на защищённые ручки")
    void unauthorized_without_token_returns_401_on_protected() throws Exception {
        mockMvc.perform(get("/order/v1/my")
                        .header("X-Brand-Id", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/kitchen/v1/ping")
                        .header("X-Brand-Id", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401: битый токен на защищённых ручках")
    void unauthorized_with_malformed_token_returns_401() throws Exception {
        String bad = "malformed.jwt.token";
        mockMvc.perform(get("/order/v1/my")
                        .header("X-Brand-Id", "1")
                        .header("Authorization", "Bearer " + bad)
                        .cookie(new Cookie("accessToken", bad)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("Authorization", "Bearer " + bad)
                        .cookie(new Cookie("accessToken", bad)))
                .andExpect(status().isUnauthorized());
    }

    // ===== 403 Forbidden =====

    @Test
    @DisplayName("403: CLIENT не может в админку и кухню")
    void forbidden_client_on_admin_and_kitchen() throws Exception {
        String username = "errs-client-" + System.nanoTime();
        Cookie login = fixtures.registerAndLogin(username);
        var clientCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.CLIENT);
        Cookie clientToken = clientCtx.cookie();
        long masterId = decodeJwt(clientToken.getValue()).path("masterId").asLong(0);

        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .cookie(clientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/kitchen/v1/ping")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .cookie(clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403: COOK не может в админку")
    void forbidden_cook_on_admin() throws Exception {
        String username = "errs-cook-" + System.nanoTime();
        Cookie login = fixtures.registerAndLogin(username);
        var cookCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.COOK);
        Cookie cookToken = cookCtx.cookie();
        long masterId = decodeJwt(cookToken.getValue()).path("masterId").asLong(0);

        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .header("Authorization", "Bearer " + cookToken.getValue())
                        .cookie(cookToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403: отсутствует X-Master-Id на защищённых ручках")
    void forbidden_missing_master_header_on_protected() throws Exception {
        String username = "errs-admin-" + System.nanoTime();
        Cookie login = fixtures.registerAndLogin(username);
        var adminCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        Cookie adminToken = adminCtx.cookie();

        // Админка без X-Master-Id
        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("Authorization", "Bearer " + adminToken.getValue())
                        .cookie(adminToken))
                .andExpect(status().isForbidden());

        // Кухня без X-Master-Id
        mockMvc.perform(get("/kitchen/v1/ping")
                        .header("X-Brand-Id", "1")
                        .header("Authorization", "Bearer " + adminToken.getValue())
                        .cookie(adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403: неверный X-Master-Id")
    void forbidden_wrong_master_header() throws Exception {
        String username = "errs-admin2-" + System.nanoTime();
        Cookie login = fixtures.registerAndLogin(username);
        var adminCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        Cookie adminToken = adminCtx.cookie();
        long wrongMaster = 9_999_999L;

        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(wrongMaster))
                        .header("Authorization", "Bearer " + adminToken.getValue())
                        .cookie(adminToken))
                .andExpect(status().isForbidden());
    }

    // ===== 404 Not Found =====

    @Test
    @DisplayName("404: запрос несуществующего заказа")
    void not_found_unknown_order_returns_404() throws Exception {
        String username = "errs-admin3-" + System.nanoTime();
        Cookie login = fixtures.registerAndLogin(username);
        var adminCtx = fixtures.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        Cookie adminToken = adminCtx.cookie();
        long masterId = decodeJwt(adminToken.getValue()).path("masterId").asLong(0);

        long absentOrderId = Long.MAX_VALUE - 1000;
        mockMvc.perform(get("/order/v1/orders/" + absentOrderId)
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .header("Authorization", "Bearer " + adminToken.getValue())
                        .cookie(adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private JsonNode decodeJwt(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return objectMapper.readTree(payload);
    }
}
