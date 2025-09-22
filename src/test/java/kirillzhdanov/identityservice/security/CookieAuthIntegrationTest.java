package kirillzhdanov.identityservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CookieAuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponse registerMinimalUser(String username, String password) throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername(username);
        req.setPassword(password);
        Set<Role.RoleName> roles = new HashSet<>();
        roles.add(Role.RoleName.USER);
        req.setRoleNames(roles);

        MvcResult res = mockMvc
                .perform(post("/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), UserResponse.class);
    }

    @Test
    @DisplayName("Аутентификация по HttpOnly cookie accessToken без Authorization → защищенный эндпоинт отвечает 200")
    void cookieAccessTokenAuthenticatesProtectedEndpoint() throws Exception {
        // Arrange: register and get access token
        UserResponse created = registerMinimalUser("cookieuser", "Password123!");
        assertThat(created.getAccessToken()).isNotBlank();

        // Act: call protected endpoint with only accessToken cookie (no Authorization header)
        String body = "{\"email\":\"test@example.com\",\"code\":null}";
        mockMvc.perform(post("/user/v1/email/verified")
                        .cookie(new Cookie("accessToken", created.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // Assert: authenticated -> 200 OK (бизнес-логика контроллера отработает)
                .andExpect(status().isOk());
    }
}
