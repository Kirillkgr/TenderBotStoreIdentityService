package kirillzhdanov.identityservice.controller.order;

import kirillzhdanov.identityservice.dto.order.OrderDto;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.order.OrderMessage;
import kirillzhdanov.identityservice.notification.longpoll.LongPollService;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderMessageRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.service.admin.OrderAdminService;
import kirillzhdanov.identityservice.config.BrandContextInterceptor;
import kirillzhdanov.identityservice.security.JwtAuthenticator;
import kirillzhdanov.identityservice.security.JwtTokenExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OrderController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration.class
        })
@Import(OrderControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderAdminService orderAdminService;
    @MockitoBean
    OrderRepository orderRepository;
    @MockitoBean
    OrderMessageRepository orderMessageRepository;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    UserBrandMembershipRepository membershipRepository;
    @MockitoBean
    LongPollService longPollService;

    @MockitoBean
    private BrandContextInterceptor brandContextInterceptor;

    @BeforeEach
    void allowRequestsThroughInterceptor() throws Exception {
        Mockito.when(brandContextInterceptor.preHandle(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(true);
    }

    @Test
    @WithMockUser(username = "user1")
    void myOrdersOk() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        Mockito.when(orderRepository.findByClient_IdOrderByIdDesc(1L)).thenReturn(List.of());
        mockMvc.perform(get("/order/v1/my")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user1")
    void getAccessibleOrdersPaged() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setUsername("user1");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        Mockito.when(membershipRepository.findBrandIdsByUserId(2L)).thenReturn(List.of(10L));
        Page<OrderDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        Mockito.when(orderAdminService.findOrdersForBrands(any(), any(), anyList(), any(), any())).thenReturn(page);
        mockMvc.perform(get("/order/v1/orders").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "admin")
    void adminSendsMessageToClient() throws Exception {
        User adminUser = new User();
        adminUser.setId(5L);
        adminUser.setUsername("admin");
        Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        // order exists, belongs to brand 10, and admin is member of brand 10
        var order = new kirillzhdanov.identityservice.model.order.Order();
        order.setId(12L);
        Brand brand = new Brand();
        brand.setId(10L);
        order.setBrand(brand);
        var client = new User();
        client.setId(1L);
        order.setClient(client);
        Mockito.when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        Mockito.when(membershipRepository.findByUser_IdAndBrand_Id(5L, 10L)).thenReturn(Optional.of(new kirillzhdanov.identityservice.model.userbrand.UserBrandMembership()));

        String body = "{\"text\":\"hi\"}";
        mockMvc.perform(post("/order/v1/orders/12/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isNoContent());

        Mockito.verify(orderMessageRepository).save(any(OrderMessage.class));
        Mockito.verify(longPollService).publishCourierMessage(eq(1L), eq(12L), eq("hi"));
    }

    @Test
    @WithMockUser(username = "user1")
    void clientSendsMessageToAdmin() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        var order = new kirillzhdanov.identityservice.model.order.Order();
        order.setId(13L);
        order.setClient(user);
        // set brand so controller notifies brand staff
        Brand brand = new Brand();
        brand.setId(10L);
        order.setBrand(brand);
        Mockito.when(orderRepository.findById(13L)).thenReturn(Optional.of(order));
        // staff users to notify
        Mockito.when(membershipRepository.findUserIdsByBrandId(10L)).thenReturn(List.of(2L, 3L));

        String body = "{\"text\":\"client\"}";
        mockMvc.perform(post("/order/v1/orders/13/client-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isNoContent());

        Mockito.verify(orderMessageRepository).save(any(OrderMessage.class));
        Mockito.verify(longPollService, Mockito.atLeastOnce()).publishClientMessage(anyLong(), eq(13L), eq("client"));
    }

    @Test
    @WithMockUser(username = "user1")
    void getOrderMessagesOkAndOrderFieldNotSerialized() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        var order = new kirillzhdanov.identityservice.model.order.Order();
        order.setId(12L);
        order.setClient(user);
        Mockito.when(orderRepository.findById(12L)).thenReturn(Optional.of(order));

        OrderMessage m = OrderMessage.builder()
                .id(1L)
                .fromClient(true)
                .text("hello")
                .senderUserId(1L)
                .build();
        Mockito.when(membershipRepository.findByUser_IdAndBrand_Id(anyLong(), anyLong())).thenReturn(Optional.of(new kirillzhdanov.identityservice.model.userbrand.UserBrandMembership()));
        Mockito.when(orderMessageRepository.findByOrder_IdOrderByIdAsc(12L)).thenReturn(List.of(m));

        mockMvc.perform(get("/order/v1/orders/12/messages").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].order").doesNotExist());
    }

    @EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
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
