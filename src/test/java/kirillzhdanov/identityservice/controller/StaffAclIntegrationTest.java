package kirillzhdanov.identityservice.controller;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("ACL: персонал (users) — admin-only CRUD")
@Transactional
class StaffAclIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MasterAccountRepository masterAccountRepository;
    @Autowired
    MembershipFixtures fx;

    Long existingUserId;
    MasterAccount master;
    String username;
    Cookie login;

    @BeforeEach
    void setUp() {
        master = masterAccountRepository.save(MasterAccount.builder().name("staff-master").build());
        User u = new User();
        u.setUsername("u-acl");
        existingUserId = userRepository.save(u).getId();
        username = "acl-staff-" + System.nanoTime();
        try {
            login = fx.registerAndLogin(username);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void admin_can_list_users() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.ADMIN, master);
        mvc.perform(get("/staff/v1/users")
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isOk());
    }

    @Test
    void client_forbidden_list_users() throws Exception {
        // Используем mock-пользователя с ролью USER, чтобы обойти глобальную роль OWNER в JWT
        mvc.perform(get("/staff/v1/users")
                        .with(user("client-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_can_create_user() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.ADMIN, master);
        String body = "{"
                + "\"lastName\":\"Иванов\","
                + "\"firstName\":\"Иван\","
                + "\"birthDate\":\"1990-01-01\","
                + "\"email\":\"ivanov@test.local\","
                + "\"phone\":\"+79990000000\","
                + "\"login\":\"u1\","
                + "\"password\":\"Password123!\","
                + "\"masterId\":" + ctx.masterId() + ","
                + "\"roles\":[\"OWNER\"]"
                + "}";
        mvc.perform(post("/staff/v1/users")
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void admin_can_update_user() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.ADMIN, master);
        String body = "{\"roles\":[\"OWNER\"]}";
        mvc.perform(put("/staff/v1/users/{id}", existingUserId)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void admin_can_delete_user() throws Exception {
        var ctx = fx.prepareRoleMembershipInMaster(login, username, RoleMembership.ADMIN, master);
        mvc.perform(delete("/staff/v1/users/{id}", existingUserId)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void client_forbidden_delete_user() throws Exception {
        // Аналогично — мок авторизации с ролью USER
        mvc.perform(delete("/staff/v1/users/{id}", existingUserId)
                        .with(user("client-user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
