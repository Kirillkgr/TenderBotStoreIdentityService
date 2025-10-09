package kirillzhdanov.identityservice.controller.profile;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.userbrand.DeliveryAddress;
import kirillzhdanov.identityservice.model.userbrand.UserBrandMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.userbrand.DeliveryAddressRepository;
import kirillzhdanov.identityservice.repository.userbrand.UserBrandMembershipRepository;
import kirillzhdanov.identityservice.testutil.CtxTestCookies;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("ACL: адреса профиля — membership/no-membership")
@Transactional
@Tag("smoke-acl")
class ProfileAddressAclIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    DeliveryAddressRepository addressRepo;
    @Autowired
    UserBrandMembershipRepository membershipRepo;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BrandRepository brandRepository;
    @Autowired
    MasterAccountRepository masterAccountRepository;
    @Autowired
    MembershipFixtures fx;

    MasterAccount master;
    Brand brand;

    String username;
    Cookie login;

    @BeforeEach
    void setUp() {
        master = masterAccountRepository.save(MasterAccount.builder().name("prof-master").build());
        brand = brandRepository.save(Brand.builder().name("prof-brand").organizationName("org-prof").master(master).build());
        username = "acl-profile-" + System.nanoTime();
        try {
            login = fx.registerAndLogin(username);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void without_membership_get_empty_and_post_delete_forbidden() throws Exception {
        Cookie ctx = CtxTestCookies.createCtx(master.getId(), brand.getId(), null, "change-me");
        // GET -> 200 empty
        mvc.perform(get("/profile/v1/addresses")
                        .cookie(login, ctx)
                        .header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // POST -> 403
        String body = "{" +
                "\"line1\":\"улица 1\"," +
                "\"city\":\"Москва\"," +
                "\"region\":\"МО\"," +
                "\"postcode\":\"101000\"" +
                "}";
        mvc.perform(post("/profile/v1/addresses")
                        .cookie(login, ctx)
                        .header("Authorization", "Bearer " + login.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // DELETE any -> 403
        mvc.perform(delete("/profile/v1/addresses/{id}", 123L)
                        .cookie(login, ctx)
                        .header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    void with_membership_crud_success_and_foreign_forbidden() throws Exception {
        // ensure membership user<->brand
        User user = userRepository.findByUsername(username).orElseThrow();
        if (membershipRepo.findByUser_IdAndBrand_Id(user.getId(), brand.getId()).isEmpty()) {
            membershipRepo.save(UserBrandMembership.builder()
                    .user(user)
                    .brand(brand)
                    .build());
        }

        Cookie ctx = CtxTestCookies.createCtx(master.getId(), brand.getId(), null, "change-me");
        // create -> 200
        String body = "{" +
                "\"line1\":\"улица 2\"," +
                "\"city\":\"Москва\"," +
                "\"region\":\"МО\"," +
                "\"postcode\":\"101000\"" +
                "}";
        mvc.perform(post("/profile/v1/addresses")
                        .cookie(login, ctx)
                        .header("Authorization", "Bearer " + login.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        // list -> 200, at least 1 item
        mvc.perform(get("/profile/v1/addresses")
                        .cookie(login, ctx)
                        .header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());

        // prepare another address owned by this membership for delete
        UserBrandMembership mb = membershipRepo.findByUser_IdAndBrand_Id(user.getId(), brand.getId()).orElseThrow();
        DeliveryAddress a = new DeliveryAddress();
        a.setMembership(mb);
        a.setLine1("line-delete");
        a.setCity("Москва");
        a.setRegion("МО");
        a.setPostcode("101000");
        a.setDeleted(Boolean.FALSE);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        Long addressId = addressRepo.save(a).getId();

        // delete own -> 204
        mvc.perform(delete("/profile/v1/addresses/{id}", addressId)
                        .cookie(login, ctx)
                        .header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isNoContent());

        // foreign delete -> 403
        String stranger = "acl-profile-str-" + System.nanoTime();
        Cookie strangerLogin;
        try {
            strangerLogin = fx.registerAndLogin(stranger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mvc.perform(delete("/profile/v1/addresses/{id}", addressId)
                        .cookie(strangerLogin, ctx)
                        .header("Authorization", "Bearer " + strangerLogin.getValue()))
                .andExpect(status().isForbidden());
    }
}
