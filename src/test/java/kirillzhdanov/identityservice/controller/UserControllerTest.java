package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kirillzhdanov.identityservice.dto.EmailVerificationRequest;
import kirillzhdanov.identityservice.dto.EmailVerifiedResponse;
import kirillzhdanov.identityservice.dto.UpdateUserRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.security.JwtTokenExtractor;
import kirillzhdanov.identityservice.security.JwtAuthenticator;
import kirillzhdanov.identityservice.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.TestConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileService userProfileService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        UserProfileService userProfileService() {
            return Mockito.mock(UserProfileService.class);
        }

        @Bean
        JwtTokenExtractor jwtTokenExtractor() {
            return Mockito.mock(JwtTokenExtractor.class);
        }

        @Bean
        JwtAuthenticator jwtAuthenticator() {
            return Mockito.mock(JwtAuthenticator.class);
        }
    }

    @Test
    @DisplayName("/user/v1/email/verified -> 401 для анонимного пользователя")
    void emailVerified_Unauthorized() throws Exception {
        EmailVerificationRequest req = new EmailVerificationRequest("test@example.com", null);
        mockMvc.perform(post("/user/v1/email/verified").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("/user/v1/email/verified -> 200 и тело ответа от сервиса")
    void emailVerified_Authorized() throws Exception {
        Mockito.when(userProfileService.checkEmailVerified(any(), any()))
                .thenReturn(EmailVerifiedResponse.builder().verified(true).build());

        EmailVerificationRequest req = new EmailVerificationRequest("test@example.com", null);
        mockMvc.perform(post("/user/v1/email/verified").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    @DisplayName("/user/v1/verifield/email POST -> 401 для анонима")
    void requestEmailCode_Unauthorized() throws Exception {
        EmailVerificationRequest req = new EmailVerificationRequest("test@example.com", null);
        mockMvc.perform(post("/user/v1/verifield/email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("/user/v1/verifield/email POST -> 204 и вызов сервиса")
    void requestEmailCode_Authorized() throws Exception {
        EmailVerificationRequest req = new EmailVerificationRequest("test@example.com", null);
        mockMvc.perform(post("/user/v1/verifield/email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
        verify(userProfileService).sendVerificationCode("user1", "test@example.com");
    }

    @Test
    @DisplayName("/user/v1/verifield/email PATCH -> 401 для анонима")
    void verifyEmail_Unauthorized() throws Exception {
        EmailVerificationRequest req = new EmailVerificationRequest("test@example.com", "123456");
        mockMvc.perform(patch("/user/v1/verifield/email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("/user/v1/verifield/email PATCH -> 200 и verified=true")
    void verifyEmail_Authorized() throws Exception {
        Mockito.when(userProfileService.verifyCode(any(), any()))
                .thenReturn(EmailVerifiedResponse.builder().verified(true).build());

        EmailVerificationRequest req = new EmailVerificationRequest("test@example.com", "123456");
        mockMvc.perform(patch("/user/v1/verifield/email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    @DisplayName("/user/v1/edit PATCH -> 401 для анонима")
    void editProfile_Unauthorized() throws Exception {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("Иван");
        mockMvc.perform(patch("/user/v1/edit").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("/user/v1/edit PATCH -> 200 и возвращает UserResponse")
    void editProfile_Authorized() throws Exception {
        UserResponse resp = UserResponse.builder()
                .id(1L)
                .username("user1")
                .firstName("Иван")
                .lastName("Иванов")
                .email("test@example.com")
                .phone("+70000000000")
                .build();
        Mockito.when(userProfileService.updateProfile(any(), any())).thenReturn(resp);

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("Иван");
        mockMvc.perform(patch("/user/v1/edit").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.firstName").value("Иван"));
    }
}
