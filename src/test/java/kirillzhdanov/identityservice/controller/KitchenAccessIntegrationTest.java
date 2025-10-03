package kirillzhdanov.identityservice.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class KitchenAccessIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipFixtures fixtures;

    @Test
    @DisplayName("B2-TS-3: COOK can access kitchen; admin area forbidden for COOK")
    void cook_access_and_admin_forbidden() throws Exception {
        String username = "cook-user-" + System.nanoTime();

        // Register and obtain initial token
        Cookie login = fixtures.registerAndLogin(username);
        assertThat(login.getValue()).isNotBlank();

        // Prepare COOK membership and switch context to get COOK token
        var ctx = fixtures.prepareRoleMembership(login, username, RoleMembership.COOK);
        Long masterId = ctx.masterId();
        Cookie cookToken = ctx.cookie();
        assertThat(cookToken.getValue()).isNotBlank();

        // Kitchen allowed for COOK
        mockMvc.perform(get("/kitchen/v1/ping")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .cookie(cookToken)
                        .header("Authorization", "Bearer " + cookToken.getValue())
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        // Admin endpoint forbidden for COOK
        mockMvc.perform(get("/admin/v1/orders")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(masterId))
                        .cookie(cookToken)
                        .header("Authorization", "Bearer " + cookToken.getValue()))
                .andExpect(status().isForbidden());
    }
}
