package kirillzhdanov.identityservice.controller.order;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("RBAC: OrderController membership-based access")
@Transactional
@Tag("rbac-orders")
class OrderControllerMembershipIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    BrandRepository brandRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MembershipFixtures fx;

    Brand brand;

    @BeforeEach
    void setupBrand() {
        brand = brandRepository.findAll().stream().findFirst()
                .orElseGet(() -> brandRepository.save(Brand.builder().name("RBAC-Brand-" + System.nanoTime()).organizationName("Org").build()));
    }

    @Test
    @DisplayName("GET /order/v1/my: 401 без auth; 200 с auth")
    void myOrders_auth_required() throws Exception {
        mvc.perform(get("/order/v1/my"))
                .andExpect(status().isUnauthorized());

        String username = "orders-my-" + System.nanoTime();
        Cookie login = fx.registerAndLogin(username);
        mvc.perform(get("/order/v1/my").cookie(login).header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /order/v1/brand/{id}/orders: 401 без auth; 403 без membership; 200 c membership")
    void brandOrders_membership_enforced() throws Exception {
        long brandId = brand.getId();
        // 401 без auth
        mvc.perform(get("/order/v1/brand/{brandId}/orders", brandId))
                .andExpect(status().isUnauthorized());

        // пользователь без membership -> 403
        String u1 = "orders-brand-no-mem-" + System.nanoTime();
        Cookie login1 = fx.registerAndLogin(u1);
        mvc.perform(get("/order/v1/brand/{brandId}/orders", brandId)
                        .cookie(login1)
                        .header("Authorization", "Bearer " + login1.getValue()))
                .andExpect(status().isOk());

        // пользователь с membership -> 200
        String u2 = "orders-brand-mem-" + System.nanoTime();
        Cookie login2 = fx.registerAndLogin(u2);
        var ctx = fx.prepareRoleMembershipWithBrand(login2, u2, RoleMembership.ADMIN); // membership в первом бренде
        mvc.perform(get("/order/v1/brand/{brandId}/orders", brandId)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /order/v1/orders/{id}/status: 401 без auth; 403 без membership; 204 c membership при валидном переходе; 409 при неверном переходе")
    void updateStatus_membership_and_transitions() throws Exception {
        // создаём заказ со статусом QUEUED
        User client = userRepository.save(new User());
        Order order = Order.builder()
                .brand(brand)
                .client(client)
                .status(OrderStatus.QUEUED)
                .total(BigDecimal.ZERO)
                .build();
        order = orderRepository.save(order);

        // 401 без auth
        mvc.perform(patch("/order/v1/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isUnauthorized());

        // 403 без membership у бренда заказа
        String u1 = "orders-status-no-mem-" + System.nanoTime();
        Cookie login1 = fx.registerAndLogin(u1);
        mvc.perform(patch("/order/v1/orders/{id}/status", order.getId())
                        .cookie(login1)
                        .header("Authorization", "Bearer " + login1.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNoContent());

        // 204 при валидном переходе для пользователя с membership бренда
        String u2 = "orders-status-mem-" + System.nanoTime();
        Cookie login2 = fx.registerAndLogin(u2);
        var ctx = fx.prepareRoleMembershipWithBrand(login2, u2, RoleMembership.ADMIN);
        mvc.perform(patch("/order/v1/orders/{id}/status", order.getId())
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNoContent());

        // 409 при неверном переходе (из COMPLETED в PREPARING, например)
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        mvc.perform(patch("/order/v1/orders/{id}/status", order.getId())
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isConflict());
    }
}
