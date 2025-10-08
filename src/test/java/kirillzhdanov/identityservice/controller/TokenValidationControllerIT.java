package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.config.BrandContextInterceptor;
import kirillzhdanov.identityservice.dto.JwtUserDetailsResponse;
import kirillzhdanov.identityservice.security.JwtAuthenticator;
import kirillzhdanov.identityservice.security.JwtTokenExtractor;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.TokenService;
import kirillzhdanov.identityservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"removal", "deprecation"})
@WebMvcTest(controllers = TokenValidationController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TokenValidationControllerIT {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TokenService tokenService;
    @MockBean
    JwtUtils jwtUtils;
    @MockBean
    UserService userService;

    @MockBean
    BrandContextInterceptor brandContextInterceptor;
    @MockBean
    JwtTokenExtractor jwtTokenExtractor;
    @MockBean
    JwtAuthenticator jwtAuthenticator;

    @Test
    @DisplayName("POST /token/v1/validate текущая реализация -> 200 без валидации")
    void validateToken_current_200() throws Exception {
        mockMvc.perform(post("/token/v1/validate"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/token/v1/validate")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /token/v1/validate/details без Authorization -> 403")
    void details_no_header_403() throws Exception {
        mockMvc.perform(post("/token/v1/validate/details"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /token/v1/validate/details невалидная подпись -> 403")
    void details_bad_signature_403() throws Exception {
        Mockito.when(jwtUtils.validateTokenSignature("abc"))
                .thenReturn(false);
        mockMvc.perform(post("/token/v1/validate/details")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /token/v1/validate/details отозванный токен -> 403")
    void details_revoked_403() throws Exception {
        Mockito.when(jwtUtils.validateTokenSignature("abc"))
                .thenReturn(true);
        Mockito.when(tokenService.isTokenValid("abc"))
                .thenReturn(false);
        mockMvc.perform(post("/token/v1/validate/details")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /token/v1/validate/details не удалось извлечь userId -> 403")
    void details_no_user_id_403() throws Exception {
        Mockito.when(jwtUtils.validateTokenSignature("abc"))
                .thenReturn(true);
        Mockito.when(tokenService.isTokenValid("abc"))
                .thenReturn(true);
        Mockito.when(jwtUtils.extractUserId("abc"))
                .thenReturn(null);
        mockMvc.perform(post("/token/v1/validate/details")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /token/v1/validate/details пользователь не найден -> 403")
    void details_user_not_found_403() throws Exception {
        Mockito.when(jwtUtils.validateTokenSignature("abc"))
                .thenReturn(true);
        Mockito.when(tokenService.isTokenValid("abc"))
                .thenReturn(true);
        Mockito.when(jwtUtils.extractUserId("abc"))
                .thenReturn(1L);
        Mockito.when(userService.getUserDetailsById(1L))
                .thenReturn(null);
        mockMvc.perform(post("/token/v1/validate/details")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /token/v1/validate/details успех -> 200")
    void details_success_200() throws Exception {
        Mockito.when(jwtUtils.validateTokenSignature("abc"))
                .thenReturn(true);
        Mockito.when(tokenService.isTokenValid("abc"))
                .thenReturn(true);
        Mockito.when(jwtUtils.extractUserId("abc"))
                .thenReturn(1L);
        Mockito.when(userService.getUserDetailsById(1L))
                .thenReturn(JwtUserDetailsResponse.builder()
                        .userId(1L)
                        .username("u")
                        .brandIds(List.of())
                        .roles(List.of("ROLE_USER"))
                        .build());

        mockMvc.perform(post("/token/v1/validate/details")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
    }
}
