package kirillzhdanov.identityservice.notification.longpoll;

import kirillzhdanov.identityservice.config.BrandContextInterceptor;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.security.JwtAuthenticator;
import kirillzhdanov.identityservice.security.JwtTokenExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = LongPollController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BrandContextInterceptor.class)
)
@AutoConfigureMockMvc(addFilters = false)
class LongPollControllerNegativeWebMvcTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    LongPollService longPollService;
    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("GET /notifications/longpoll unauthenticated -> 200 (slice без фильтров)")
    void longpoll_unauthenticated_returns204() throws Exception {
        mockMvc.perform(get("/notifications/longpoll").param("since", "0"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/longpoll authenticated but service throws -> 204 (slice)")
    @WithMockUser(username = "user1")
    void longpoll_exception_mapped_204() throws Exception {
        var u = new kirillzhdanov.identityservice.model.User();
        u.setId(42L);
        u.setUsername("user1");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(Optional.of(u));
        CompletableFuture<LongPollEnvelope> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));
        Mockito.when(longPollService.poll(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(failed);

        mockMvc.perform(get("/notifications/longpoll").param("since", "7"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /notifications/longpoll/ack unauthenticated -> 204 (slice)")
    void ack_unauthenticated_returns204() throws Exception {
        mockMvc.perform(post("/notifications/longpoll/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReceivedId\": 10}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /notifications/longpoll/ack authenticated but service throws -> 204 (slice)")
    @WithMockUser(username = "user1")
    void ack_exception_mapped_204() throws Exception {
        var u = new kirillzhdanov.identityservice.model.User();
        u.setId(42L);
        u.setUsername("user1");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(Optional.of(u));
        Mockito.doThrow(new RuntimeException("boom")).when(longPollService).ack(Mockito.anyLong(), Mockito.anyLong());

        mockMvc.perform(post("/notifications/longpoll/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReceivedId\": 99}"))
                .andExpect(status().isNoContent());
    }

    @TestConfiguration
    static class MocksConfig {
        @Bean
        LongPollService longPollService() {
            return Mockito.mock(LongPollService.class);
        }

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        BrandRepository brandRepository() {
            return Mockito.mock(BrandRepository.class);
        }

        @Bean
        JwtTokenExtractor jwtTokenExtractor() {
            return Mockito.mock(JwtTokenExtractor.class);
        }

        @Bean
        JwtAuthenticator jwtAuthenticator() {
            return Mockito.mock(JwtAuthenticator.class);
        }

        @Bean
        BrandContextInterceptor brandContextInterceptor() {
            return Mockito.mock(BrandContextInterceptor.class);
        }
    }
}
