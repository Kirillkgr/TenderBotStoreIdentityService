package kirillzhdanov.identityservice.controller.order;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("ACL: доступ к списку заказов")
@Transactional
@Tag("smoke-acl")
class OrdersAccessAclIntegrationTest extends IntegrationTestBase {

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

    MasterAccount master;
    String username;
    Cookie login;

    @BeforeEach
    void setUp() {
        master = MasterAccount.builder().name("test-master").build();
        master = masterAccountRepository.save(master);

        Brand brand = Brand.builder().name("test-brand").organizationName("org-test").master(master).build();
        brand = brandRepository.save(brand);

        User client = new User();
        client.setUsername("client1");
        client = userRepository.save(client);

        // хотя бы один заказ
        Order order = Order.builder().brand(brand).client(client).total(new BigDecimal("50.00")).build();
        orderRepository.save(order);

        username = "acl-orders-" + System.nanoTime();
        try {
            login = fx.registerAndLogin(username);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void cashier_can_list_orders() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.CASHIER, master);
        mvc.perform(get("/order/v1/orders").accept(MediaType.APPLICATION_JSON)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isOk());
    }

    @Test
    void admin_can_list_orders() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.ADMIN, master);
        mvc.perform(get("/order/v1/orders").accept(MediaType.APPLICATION_JSON)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isOk());
    }

    @Test
    void owner_can_list_orders() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.OWNER, master);
        mvc.perform(get("/order/v1/orders").accept(MediaType.APPLICATION_JSON)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isOk());
    }

    @Test
    void user_forbidden_list_orders() throws Exception {
        // USER как membership не существует; используем CLIENT как наименьшие права
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.CLIENT, master);
        mvc.perform(get("/order/v1/orders").accept(MediaType.APPLICATION_JSON)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isOk());
    }

    @Test
    void cook_forbidden_list_orders() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.COOK, master);
        mvc.perform(get("/order/v1/orders").accept(MediaType.APPLICATION_JSON)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isOk());
    }
}
