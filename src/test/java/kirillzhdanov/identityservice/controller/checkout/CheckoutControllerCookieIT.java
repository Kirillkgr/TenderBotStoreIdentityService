package kirillzhdanov.identityservice.controller.checkout;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.BrandContextInterceptor;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.order.DeliveryMode;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.security.JwtAuthenticator;
import kirillzhdanov.identityservice.security.JwtTokenExtractor;
import kirillzhdanov.identityservice.service.CheckoutService;
import kirillzhdanov.identityservice.tenant.CtxCookieFilter;
import kirillzhdanov.identityservice.testutil.CtxTestCookies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CheckoutController.class)
@Import({CtxCookieFilter.class})
@AutoConfigureMockMvc(addFilters = true)
class CheckoutControllerCookieIT {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CheckoutService checkoutService;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    OrderRepository orderRepository;
    @MockitoBean
    BrandContextInterceptor brandContextInterceptor;
    @MockitoBean
    JwtTokenExtractor jwtTokenExtractor;
    @MockitoBean
    JwtAuthenticator jwtAuthenticator;

    @Test
    @WithMockUser(username = "u1")
    @DisplayName("PICKUP без pickupPointId, с ctx(masterId) -> 400 PICKUP_POINT_REQUIRED")
    void pickup_without_ctx_returns400() throws Exception {
        Mockito.when(userRepository.findByUsername("u1")).thenReturn(java.util.Optional.of(new User()));

        // Для прохождения ContextEnforcementFilter требуется masterId в контексте
        Cookie ctxMasterOnly = CtxTestCookies.createCtx(1L, null, null, "change-me");

        mockMvc.perform(post("/checkout").with(csrf())
                        .cookie(new Cookie("cart_token", "ct-1"), ctxMasterOnly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"PICKUP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("PICKUP_POINT_REQUIRED")));
    }

    @Test
    @WithMockUser(username = "u1")
    @DisplayName("PICKUP с pickupPointId из ctx -> 200 и создаётся заказ")
    void pickup_with_ctx_creates_order() throws Exception {
        User u = new User();
        u.setId(10L);
        u.setUsername("u1");
        Mockito.when(userRepository.findByUsername("u1")).thenReturn(java.util.Optional.of(u));

        Order order = new Order();
        order.setId(1L);
        Mockito.when(checkoutService.createOrderFromCart(Mockito.any(), Mockito.eq(DeliveryMode.PICKUP), Mockito.isNull(), Mockito.eq(777L), Mockito.isNull(), Mockito.anyString()))
                .thenReturn(order);

        Cookie ctx = CtxTestCookies.createCtx(1L, null, 777L, "change-me");

        mockMvc.perform(post("/checkout").with(csrf())
                        .cookie(new Cookie("cart_token", "ct-2"), ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"PICKUP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

}
