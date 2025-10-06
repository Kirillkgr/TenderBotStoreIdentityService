package kirillzhdanov.identityservice.controller.order;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.model.order.Order;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("ACL: сообщения по заказу — staff vs client")
@Transactional
class OrderMessagesAclIntegrationTest extends IntegrationTestBase {

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
    UserMembershipRepository membershipRepo;
    @Autowired
    UserBrandMembershipRepository userBrandMembershipRepository;
    @Autowired
    MembershipFixtures fx;

    Long orderId;
    MasterAccount master;
    Brand brand;

    String staffUsername;
    Cookie staffLogin;

    String clientUsername;
    Cookie clientLogin;

    @BeforeEach
    void setUp() {
        // master/brand
        master = masterAccountRepository.save(MasterAccount.builder().name("msg-master").build());
        brand = brandRepository.save(Brand.builder().name("msg-brand").organizationName("org-msg").master(master).build());

        // register client first and use this persisted user as order client
        clientUsername = "acl-msg-client-" + System.nanoTime();
        try {
            clientLogin = fx.registerAndLogin(clientUsername);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        User client = userRepository.findByUsername(clientUsername).orElseThrow();

        // create order bound to brand and real client
        Order order = Order.builder().brand(brand).client(client).total(new java.math.BigDecimal("10.00")).build();
        orderId = orderRepository.save(order).getId();

        // register staff
        staffUsername = "acl-msg-staff-" + System.nanoTime();
        try {
            staffLogin = fx.registerAndLogin(staffUsername);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Cookie switchStaff(RoleMembership role) throws Exception {
        Long userId = userRepository.findByUsername(staffUsername).orElseThrow().getId();
        UserMembership um = new UserMembership();
        um.setUser(userRepository.findById(userId).orElseThrow());
        um.setMaster(master);
        um.setRole(role);
        try {
            um.setBrand(brand);
        } catch (Exception ignored) {
        }
        Long memId = membershipRepo.save(um).getId();
        // Ensure brand-level membership for ACL (staff side)
        if (userBrandMembershipRepository.findByUser_IdAndBrand_Id(userId, brand.getId()).isEmpty()) {
            userBrandMembershipRepository.save(UserBrandMembership.builder()
                    .user(userRepository.findById(userId).orElseThrow())
                    .brand(brand)
                    .build());
        }
        return fx.switchContext(staffLogin, memId);
    }

    @Test
    void staff_can_send_courier_message() throws Exception {
        Cookie ctx = switchStaff(RoleMembership.ADMIN);
        mvc.perform(post("/order/v1/orders/{id}/message", orderId)
                        .cookie(ctx)
                        .header("Authorization", "Bearer " + ctx.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void client_cannot_send_courier_message() throws Exception {
        // client tries to use staff endpoint -> 403
        mvc.perform(post("/order/v1/orders/{id}/message", orderId)
                        .cookie(clientLogin)
                        .header("Authorization", "Bearer " + clientLogin.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void client_can_send_client_message() throws Exception {
        // client endpoint should be allowed for the actual order client
        mvc.perform(post("/order/v1/orders/{id}/client-message", orderId)
                        .cookie(clientLogin)
                        .header("Authorization", "Bearer " + clientLogin.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"from client\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void stranger_forbidden_send_client_message() throws Exception {
        // register another user who is not the order client
        String stranger = "acl-msg-stranger-" + System.nanoTime();
        Cookie strangerLogin;
        try {
            strangerLogin = fx.registerAndLogin(stranger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mvc.perform(post("/order/v1/orders/{id}/client-message", orderId)
                        .cookie(strangerLogin)
                        .header("Authorization", "Bearer " + strangerLogin.getValue())
                        .header("X-Master-Id", master.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"not allowed\"}"))
                .andExpect(status().isForbidden());
    }
}
