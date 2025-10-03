package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AuthWhoamiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MembershipFixtures fixtures;

    @Test
    @DisplayName("whoami: 401 без авторизации")
    void whoami_unauthorized() throws Exception {
        mockMvc.perform(get("/auth/v1/whoami").header("X-Brand-Id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("whoami: 200 с валидным access token после регистрации")
    void whoami_ok_afterRegisterAndLogin() throws Exception {
        String username = "whoami-user-" + System.nanoTime();
        // Регистрация
        String reg = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + username + "@test.local\"," +
                "\"password\":\"Password123!\"," +
                "\"roleNames\":[\"USER\"]}";
        mockMvc.perform(post("/auth/v1/register")
                        .header("X-Brand-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reg.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated());

        // Логин по basic
        String basic = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((username + ":Password123!").getBytes(StandardCharsets.UTF_8));
        MvcResult loginRes = mockMvc.perform(post("/auth/v1/login")
                        .header("X-Brand-Id", "1")
                        .header("Authorization", basic))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        String accessToken = body.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        // whoami под авторизацией
        MvcResult who = mockMvc.perform(get("/auth/v1/whoami")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", "1")
                        .cookie(new Cookie("accessToken", accessToken))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode profile = objectMapper.readTree(who.getResponse().getContentAsString());
        assertThat(profile.get("username").asText()).isEqualTo(username);
    }

    @Test
    @DisplayName("whoami: 200 с валидным access token (через фикстуру registerAndLogin)")
    void whoami_ok_afterFixturesLogin() throws Exception {
        String username = "fx-user-" + System.nanoTime();
        Cookie login = fixtures.registerAndLogin(username);
        assertThat(login.getValue()).isNotBlank();

        mockMvc.perform(get("/auth/v1/whoami")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", "1")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("whoami: 401 с невалидным токеном")
    void whoami_unauthorized_withInvalidToken() throws Exception {
        mockMvc.perform(get("/auth/v1/whoami")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", "1")
                        .cookie(new Cookie("accessToken", "invalid"))
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
}
