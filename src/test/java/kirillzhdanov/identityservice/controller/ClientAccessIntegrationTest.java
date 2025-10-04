package kirillzhdanov.identityservice.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ClientAccessIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembershipFixtures fixtures;

    @Test
    @DisplayName("CLIENT: доступ к /order/v1/my (200), запрет /admin/v1/orders и каталога (403)")
    void client_access_and_forbidden_admin_catalog() throws Exception {
        String username = "client-user-" + System.nanoTime();

        // Register and obtain initial token
        Cookie login = fixtures.registerAndLogin(username);
        assertThat(login.getValue()).isNotBlank();

        // Prepare CLIENT membership and switch context to get CLIENT token
        var ctx = fixtures.prepareRoleMembership(login, username, RoleMembership.CLIENT);
        Long masterId = ctx.masterId();
        Cookie clientToken = ctx.cookie();
        assertThat(clientToken.getValue()).isNotBlank();

        // Allowed: client orders
        mockMvc.perform(get("/order/v1/my")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue()))
                .andExpect(status().isOk());

        // Forbidden: admin orders
        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue()))
                .andExpect(status().isForbidden());

        // Forbidden: catalog create product
        String body = """
                {
                  "name": "Test Product",
                  "price": 10.0,
                  "brandId": 1
                }""";
        mockMvc.perform(post("/auth/v1/products")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
