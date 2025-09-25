package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.config.BrandContextInterceptor;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.cart.CartItemRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CartController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BrandContextInterceptor.class)
)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerWebMvcSmokeTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    ProductRepository productRepository;

    @Test
    @DisplayName("GET /cart гостем: 200, пустая корзина и cookie cart_token")
    void getCart_guest_ok() throws Exception {
        Mockito.when(cartItemRepository.findByCartToken(Mockito.anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /cart/add без productId -> 400")
    void add_missingProduct_badRequest() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /cart/add с несуществующим товаром -> 400")
    void add_productNotFound_badRequest() throws Exception {
        Mockito.when(productRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());
        String body = "{\"productId\": 123, \"quantity\": 1}";
        mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /cart/remove/{id} когда элемента нет -> 204")
    void remove_notFound_noContent() throws Exception {
        Mockito.when(cartItemRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());
        mockMvc.perform(delete("/cart/remove/{id}", 999L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /cart/item/{id} когда элемента нет -> 204")
    void patch_notFound_noContent() throws Exception {
        Mockito.when(cartItemRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());
        mockMvc.perform(patch("/cart/item/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class MocksConfig {
        @Bean
        CartItemRepository cartItemRepository() {
            return Mockito.mock(CartItemRepository.class);
        }

        @Bean
        ProductRepository productRepository() {
            return Mockito.mock(ProductRepository.class);
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

        @Bean
        CartController cartController(CartItemRepository cartRepo, ProductRepository productRepo, UserRepository userRepo, BrandRepository brandRepo) {
            return new CartController(cartRepo, productRepo, userRepo, brandRepo);
        }
    }
}
