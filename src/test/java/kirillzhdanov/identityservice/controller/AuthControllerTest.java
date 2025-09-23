package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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

    private String loginAndGetRefreshTokenCookie() throws Exception {
        String credentials = Base64.getEncoder().encodeToString(("testuser:Password123!").getBytes());
        MvcResult loginResult = mockMvc.perform(post("/auth/v1/login").header("Authorization", "Basic " + credentials))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = loginResult.getResponse().getCookie("refreshToken");
        return cookie != null ? cookie.getValue() : null;
    }

    private UserResponse registerAndGetResponse() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/v1/register")
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
        mockMvc.perform(post("/auth/v1/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("Регистрация пользователя - имя пользователя уже существует")
    void registerUser_UsernameAlreadyExists() throws Exception {
        registerAndGetResponse(); // First registration

        mockMvc.perform(post("/auth/v1/register").contentType(MediaType.APPLICATION_JSON) // Second registration
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

        mockMvc.perform(post("/auth/v1/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Вход пользователя - успешно")
    void login_Success() throws Exception {
        registerAndGetResponse();
        String credentials = Base64.getEncoder().encodeToString(("testuser:Password123!").getBytes());

        mockMvc.perform(post("/auth/v1/login")
                        .header("Authorization", "Basic " + credentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                // refreshToken уходит в Set-Cookie, а не в body
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=")));
    }

    @Test
    @DisplayName("Вход пользователя - неверные учетные данные")
    void login_InvalidCredentials() throws Exception {
        registerAndGetResponse();
        String wrongCredentials = Base64.getEncoder().encodeToString("testuser:wrongpassword".getBytes());

        mockMvc.perform(post("/auth/v1/login")
                        .header("Authorization", "Basic " + wrongCredentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Обновление токена - успешно")
    void refreshToken_Success() throws Exception {
        registerAndGetResponse();
        String refresh = loginAndGetRefreshTokenCookie();
        org.junit.jupiter.api.Assertions.assertNotNull(refresh, "Refresh token cookie must be present after login");
        mockMvc.perform(post("/auth/v1/refresh")
                        .cookie(new Cookie("refreshToken", refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                // refreshToken теперь не возвращается в теле (HttpOnly cookie), ожидаем null
                .andExpect(jsonPath("$.refreshToken", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("Обновление токена - недействительный токен")
    void refreshToken_InvalidToken() throws Exception {
        mockMvc.perform(post("/auth/v1/refresh")
                        .cookie(new Cookie("refreshToken", "invalid-refresh-token")))
                .andExpect(status().isForbidden())
                ;
    }

    @Test
    @DisplayName("Отзыв токена - успешно")
    @WithMockUser(username = "testuser", roles = "USER")
    void revokeToken_Success() throws Exception {
        UserResponse userResponse = registerAndGetResponse();

        mockMvc.perform(post("/auth/v1/revoke").param("token", userResponse.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Отзыв всех токенов пользователя - успешно")
    @WithMockUser(roles = "ADMIN")
    void revokeAllUserTokens_Success() throws Exception {
        registerAndGetResponse();

        mockMvc.perform(post("/auth/v1/revoke-all").param("username", "testuser"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Выход из системы - успешно")
    @WithMockUser(username = "testuser")
    void logout_Success() throws Exception {
        UserResponse userResponse = registerAndGetResponse();

        mockMvc.perform(delete("/auth/v1/logout").contentType(MediaType.TEXT_PLAIN)
                        .content(userResponse.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Выход из всех сессий - успешно")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void logoutAll_Success() throws Exception {
        registerAndGetResponse();

        mockMvc.perform(delete("/auth/v1/logout/all/{username}", "testuser"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Refresh: выполняется ротация refresh-токена, новый устанавливается в cookie, старый становится недействительным")
    void refresh_rotates_refresh_token_and_invalidates_old() throws Exception {
        // Arrange: register and login to obtain initial refresh cookie
        registerAndGetResponse();
        String credentials = Base64.getEncoder().encodeToString(("testuser:Password123!").getBytes());
        MvcResult loginRes = mockMvc.perform(post("/auth/v1/login").header("Authorization", "Basic " + credentials))
                .andExpect(status().isOk())
                .andReturn();
        Cookie oldRefreshCookie = loginRes.getResponse().getCookie("refreshToken");
        org.junit.jupiter.api.Assertions.assertNotNull(oldRefreshCookie, "refreshToken cookie must be present after login");

        // Act: call refresh with old refresh cookie
        MvcResult refreshRes = mockMvc.perform(post("/auth/v1/refresh")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn();

        // Extract new refresh cookie from Set-Cookie
        Cookie newRefreshCookie = refreshRes.getResponse().getCookie("refreshToken");
        org.junit.jupiter.api.Assertions.assertNotNull(newRefreshCookie, "new refreshToken cookie must be set on refresh");
        org.junit.jupiter.api.Assertions.assertNotEquals(oldRefreshCookie.getValue(), newRefreshCookie.getValue(), "refresh token must rotate");

        // Assert: old refresh becomes invalid -> second refresh with the old cookie is forbidden
        mockMvc.perform(post("/auth/v1/refresh")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isForbidden());
    }
}
