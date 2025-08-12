package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.TokenRefreshRequest;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setUsername("testuser");
        registrationRequest.setPassword("Password123!");
        Set<Role.RoleName> roles = new HashSet<>();
        roles.add(Role.RoleName.USER);
        registrationRequest.setRoleNames(roles);
    }

    private UserResponse registerAndGetResponse() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        return objectMapper.readValue(content, UserResponse.class);
    }

    @Test
    @DisplayName("Регистрация пользователя - успешно")
    void registerUser_Success() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    @DisplayName("Регистрация пользователя - имя пользователя уже существует")
    void registerUser_UsernameAlreadyExists() throws Exception {
        registerAndGetResponse(); // First registration

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON) // Second registration
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Пользователь с таким именем уже существует")));
    }

    @Test
    @DisplayName("Регистрация пользователя - невалидные данные")
    void registerUser_InvalidData() throws Exception {
        UserRegistrationRequest invalidRequest = new UserRegistrationRequest();
        invalidRequest.setUsername("");
        invalidRequest.setPassword("123");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Вход пользователя - успешно")
    void login_Success() throws Exception {
        registerAndGetResponse();
        String credentials = Base64.getEncoder().encodeToString(("testuser:Password123!").getBytes());

        mockMvc.perform(post("/auth/login")
                        .header("Authorization", "Basic " + credentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    @DisplayName("Вход пользователя - неверные учетные данные")
    void login_InvalidCredentials() throws Exception {
        registerAndGetResponse();
        String wrongCredentials = Base64.getEncoder().encodeToString("testuser:wrongpassword".getBytes());

        mockMvc.perform(post("/auth/login")
                        .header("Authorization", "Basic " + wrongCredentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Обновление токена - успешно")
    void refreshToken_Success() throws Exception {
        UserResponse userResponse = registerAndGetResponse();
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
        refreshRequest.setRefreshToken(userResponse.getRefreshToken());

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", is(userResponse.getRefreshToken())));
    }

    @Test
    @DisplayName("Обновление токена - недействительный токен")
    void refreshToken_InvalidToken() throws Exception {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Токен обновления недействителен или истек")));
    }

    @Test
    @DisplayName("Отзыв токена - успешно")
    @WithMockUser(username = "testuser", roles = "USER")
    void revokeToken_Success() throws Exception {
        UserResponse userResponse = registerAndGetResponse();

        mockMvc.perform(post("/auth/revoke").param("token", userResponse.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Отзыв всех токенов пользователя - успешно")
    @WithMockUser(roles = "ADMIN")
    void revokeAllUserTokens_Success() throws Exception {
        registerAndGetResponse();

        mockMvc.perform(post("/auth/revoke-all").param("username", "testuser"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Выход из системы - успешно")
    void logout_Success() throws Exception {
        UserResponse userResponse = registerAndGetResponse();

        mockMvc.perform(delete("/auth/logout").contentType(MediaType.TEXT_PLAIN)
                        .content(userResponse.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Выход из всех сессий - успешно")
    void logoutAll_Success() throws Exception {
        registerAndGetResponse();

        mockMvc.perform(delete("/auth/logout/all/{username}", "testuser"))
                .andExpect(status().isOk());
    }
}
