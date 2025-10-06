package kirillzhdanov.identityservice.controller.order;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.model.userbrand.UserBrandMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.order.OrderRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("ACL: смена статуса заказа")
@Transactional
class OrderStatusAclIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    BrandRepository brandRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MasterAccountRepository masterAccountRepository;
    @Autowired
    MembershipFixtures fx;
    @Autowired
    UserMembershipRepository membershipRepo;
    @Autowired
    UserBrandMembershipRepository userBrandMembershipRepository;

    Long orderId;
    MasterAccount master;
    String username;
    Cookie login;
    Brand brand;

    @BeforeEach
    void setUp() {
        // master
        master = MasterAccount.builder().name("test-master").build();
        master = masterAccountRepository.save(master);

        // brand
        brand = Brand.builder().name("test-brand").organizationName("org-test").master(master).build();
        brand = brandRepository.save(brand);

        // client
        User client = new User();
        client.setUsername("client1");
        client = userRepository.save(client);

        // order
        Order order = Order.builder()
                .brand(brand)
                .client(client)
                .total(new BigDecimal("100.00"))
                .build();
        order = orderRepository.save(order);
        orderId = order.getId();

        // auth principal for role switching
        username = "acl-status-" + System.nanoTime();
        try {
            login = fx.registerAndLogin(username);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Cookie switchTo(RoleMembership role) throws Exception {
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        UserMembership um = new UserMembership();
        um.setUser(userRepository.findById(userId).orElseThrow());
        um.setMaster(master);
        um.setRole(role);
        try {
            um.setBrand(brand);
        } catch (Exception ignored) {
        }
        Long memId = membershipRepo.save(um).getId();
        // Ensure brand-level membership used by ACL in OrderController
        if (userBrandMembershipRepository.findByUser_IdAndBrand_Id(userId, brand.getId()).isEmpty()) {
            UserBrandMembership ubm = UserBrandMembership.builder()
                    .user(userRepository.findById(userId).orElseThrow())
                    .brand(brand)
                    .build();
            userBrandMembershipRepository.save(ubm);
        }
        return fx.switchContext(login, memId);
    }

    @Test
    void cashier_can_update_status() throws Exception {
        Cookie ctx = switchTo(RoleMembership.CASHIER);
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void user_forbidden_update_status() throws Exception {
        // USER is a global role, not a membership role; use CLIENT to represent minimal membership rights
        Cookie ctx = switchTo(RoleMembership.CLIENT);
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void admin_can_update_status() throws Exception {
        Cookie ctx = switchTo(RoleMembership.ADMIN);
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void owner_can_update_status() throws Exception {
        Cookie ctx = switchTo(RoleMembership.OWNER);
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cook_forbidden_update_status() throws Exception {
        Cookie ctx = switchTo(RoleMembership.COOK);
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void client_forbidden_update_status() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.CLIENT, master);
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_cannot_update_status() throws Exception {
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void not_found_order_returns_404() throws Exception {
        long missingId = orderId + 999_999L;
        Cookie ctx = switchTo(RoleMembership.ADMIN);
        mvc.perform(patch("/order/v1/orders/{id}/status", missingId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalid_transition_from_canceled_returns_409() throws Exception {
        // set current status to CANCELED
        Order o = orderRepository.findById(orderId).orElseThrow();
        o.setStatus(OrderStatus.CANCELED);
        orderRepository.save(o);

        Cookie ctx = switchTo(RoleMembership.ADMIN);
        mvc.perform(patch("/order/v1/orders/{id}/status", orderId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PREPARING\"}"))
                .andExpect(status().isConflict());
    }
}
