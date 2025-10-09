package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.config.BrandContextInterceptor;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.security.CustomUserDetails;
import kirillzhdanov.identityservice.security.JwtAuthenticator;
import kirillzhdanov.identityservice.security.JwtTokenExtractor;
import kirillzhdanov.identityservice.service.UserProviderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountLinkController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class})
@Import(AccountLinkControllerAuthIT.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountLinkControllerAuthIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void unlink_google_unauth_401() throws Exception {
        // без аутентификации (аноним)
        SecurityContextHolder.clearContext();
        mockMvc.perform(delete("/auth/v1/providers/google").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /auth/v1/providers/google аутентификация есть, но пользователь не найден -> 401")
    void unlink_google_user_not_found_401() throws Exception {
        var u = new User();
        u.setId(1L);
        var cud = new CustomUserDetails(u);
        var auth = new UsernamePasswordAuthenticationToken(cud, "p", cud.getAuthorities());

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/auth/v1/providers/google").with(authentication(auth)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /auth/v1/providers/google успех -> 204")
    void unlink_google_ok_204() throws Exception {
        var u = new User();
        u.setId(1L);
        var cud = new CustomUserDetails(u);
        var auth = new UsernamePasswordAuthenticationToken(cud, "p", cud.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));

        mockMvc.perform(delete("/auth/v1/providers/google").with(authentication(auth)))
                .andExpect(status().isNoContent());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        @Primary
        UserProviderService userProviderService() {
            return Mockito.mock(UserProviderService.class);
        }

        @Bean
        @Primary
        BrandContextInterceptor brandContextInterceptor() {
            return Mockito.mock(BrandContextInterceptor.class);
        }

        @Bean
        @Primary
        JwtTokenExtractor jwtTokenExtractor() {
            return Mockito.mock(JwtTokenExtractor.class);
        }

        @Bean
        @Primary
        JwtAuthenticator jwtAuthenticator() {
            return Mockito.mock(JwtAuthenticator.class);
        }
    }
}
