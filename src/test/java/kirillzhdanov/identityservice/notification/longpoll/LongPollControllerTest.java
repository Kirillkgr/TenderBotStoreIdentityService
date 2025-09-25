package kirillzhdanov.identityservice.notification.longpoll;

import com.fasterxml.jackson.databind.ObjectMapper;
import kirillzhdanov.identityservice.config.BrandContextInterceptor;
import kirillzhdanov.identityservice.security.JwtAuthenticator;
import kirillzhdanov.identityservice.security.JwtTokenExtractor;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(controllers = LongPollController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration.class
        })
@Import(LongPollControllerTest.TestConfig.class)
class LongPollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LongPollService longPollService;

    @MockitoBean
    private UserRepository userRepository;

    // Interceptor is part of app config; allow requests to pass in slice test
    @MockitoBean
    private BrandContextInterceptor brandContextInterceptor;

    @BeforeEach
    void allowRequestsThroughInterceptor() throws Exception {
        Mockito.when(brandContextInterceptor.preHandle(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("GET /notifications/longpoll -> 204 для анонима (idle-ответ вместо 401)")
    void poll_Unauthenticated_NoContent() throws Exception {
        var mvcResult = mockMvc.perform(get("/notifications/longpoll"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());
        verifyNoInteractions(longPollService);
    }

    @Test
    @WithMockUser(username = "123")
    @DisplayName("GET /notifications/longpoll -> 200 и тело, когда есть события; userId резолвится как число")
    void poll_Authenticated_WithEvents_Ok() throws Exception {
        LongPollEvent evt = LongPollEvent.builder()
                .id(10)
                .type(LongPollEventType.CLIENT_MESSAGE)
                .orderId(5L)
                .text("hi")
                .build();
        LongPollEnvelope env = LongPollEnvelope.builder()
                .events(List.of(evt))
                .nextSince(10)
                .hasMore(false)
                .build();
        Mockito.when(longPollService.poll(eq(123L), anyLong(), anyLong(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(env));

        var mvcResult = mockMvc.perform(get("/notifications/longpoll")
                        .param("since", "0")
                        .param("timeoutMs", "1000")
                        .param("maxBatch", "10"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].type", equalTo("CLIENT_MESSAGE")))
                .andExpect(jsonPath("$.nextSince", equalTo(10)))
                .andExpect(jsonPath("$.hasMore", equalTo(false)));

        verify(longPollService).poll(eq(123L), eq(0L), eq(1000L), eq(10));
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("GET /notifications/longpoll -> резолвим userId по username через репозиторий")
    void poll_Authenticated_ResolveUserIdByUsername() throws Exception {
        Mockito.when(userRepository.findByUsername("user1"))
                .thenReturn(Optional.of(User.builder().id(777L).username("user1").build()));

        LongPollEnvelope env = LongPollEnvelope.builder()
                .events(List.of()) // idle -> контроллер вернёт 204
                .nextSince(0)
                .hasMore(false)
                .build();
        Mockito.when(longPollService.poll(eq(777L), anyLong(), anyLong(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(env));

        var mvcResult = mockMvc.perform(get("/notifications/longpoll").param("since", "5"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());

        verify(userRepository).findByUsername("user1");
        verify(longPollService).poll(eq(777L), eq(5L), anyLong(), anyInt());
    }

    @Test
    @WithMockUser(username = "123")
    @DisplayName("GET /notifications/longpoll -> 204 если сервис вернул пустой список событий (idle)")
    void poll_Authenticated_EmptyEvents_NoContent() throws Exception {
        LongPollEnvelope env = LongPollEnvelope.builder()
                .events(List.of())
                .nextSince(0)
                .hasMore(false)
                .build();
        Mockito.when(longPollService.poll(eq(123L), anyLong(), anyLong(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(env));

        var mvcResult = mockMvc.perform(get("/notifications/longpoll"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "123")
    @DisplayName("GET /notifications/longpoll -> 204 если сервис завершился исключением (mapped)")
    void poll_Authenticated_ServiceException_NoContent() throws Exception {
        CompletableFuture<LongPollEnvelope> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));
        Mockito.when(longPollService.poll(eq(123L), anyLong(), anyLong(), anyInt()))
                .thenReturn(failed);
        var mvcResult = mockMvc.perform(get("/notifications/longpoll"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /notifications/longpoll/ack -> 204 для анонима и no-op")
    void ack_Unauthenticated_NoContent() throws Exception {
        LongPollAckRequest req = LongPollAckRequest.builder().lastReceivedId(15).build();
        mockMvc.perform(post("/notifications/longpoll/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verifyNoInteractions(longPollService);
    }

    @Test
    @WithMockUser(username = "55")
    @DisplayName("POST /notifications/longpoll/ack -> 204 и вызывает сервис с numeric userId")
    void ack_Authenticated_CallsService() throws Exception {
        LongPollAckRequest req = LongPollAckRequest.builder().lastReceivedId(33).build();
        mockMvc.perform(post("/notifications/longpoll/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> lastIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(longPollService).ack(userIdCaptor.capture(), lastIdCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(55L, userIdCaptor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals(33L, lastIdCaptor.getValue());
    }

    @Test
    @WithMockUser(username = "user2")
    @DisplayName("POST /notifications/longpoll/ack -> резолвим userId по username")
    void ack_Authenticated_ResolveUserIdByUsername() throws Exception {
        Mockito.when(userRepository.findByUsername("user2"))
                .thenReturn(Optional.of(User.builder().id(900L).username("user2").build()));

        LongPollAckRequest req = LongPollAckRequest.builder().lastReceivedId(7).build();
        mockMvc.perform(post("/notifications/longpoll/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(longPollService).ack(900L, 7L);
        verify(userRepository).findByUsername("user2");
    }

    static class TestConfig {
        @Bean
        JwtTokenExtractor jwtTokenExtractor() {
            return Mockito.mock(JwtTokenExtractor.class);
        }

        @Bean
        JwtAuthenticator jwtAuthenticator() {
            return Mockito.mock(JwtAuthenticator.class);
        }

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(reg -> reg
                            // Делаем longpoll endpoint публичным как в контроллере
                            .requestMatchers("/notifications/longpoll", "/notifications/longpoll/ack").permitAll()
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((req, res, e) -> res.sendError(401))
                            .accessDeniedHandler((req, res, e) -> res.sendError(403))
                    )
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }
    }
}
