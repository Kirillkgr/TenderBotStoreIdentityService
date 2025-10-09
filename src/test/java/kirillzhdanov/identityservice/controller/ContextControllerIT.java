package kirillzhdanov.identityservice.controller;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import kirillzhdanov.identityservice.util.HmacSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ContextControllerIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    MembershipFixtures fx;
    @Autowired
    MasterAccountRepository masterRepo;
    @Autowired
    BrandRepository brandRepo;
    @Autowired
    UserMembershipRepository membershipRepo;
    @Autowired
    UserRepository userRepository;

    Cookie login;
    String username;
    MasterAccount master;
    Brand brand;

    @BeforeEach
    void setup() throws Exception {
        username = "ctx-it-" + System.nanoTime();
        login = fx.registerAndLogin(username);
        master = masterRepo.save(MasterAccount.builder().name("m-ctx").status("ACTIVE").build());
        brand = brandRepo.save(Brand.builder()
                .name("b-ctx-" + System.nanoTime())
                .organizationName("org-" + System.nanoTime())
                .master(master)
                .build());
        // Привяжем пользователя к бренду, чтобы setContext прошёл валидацию
        User user = userRepository.findByUsername(username).orElseThrow();
        if (membershipRepo.findByUserIdAndBrandId(user.getId(), brand.getId()).isEmpty()) {
            membershipRepo.save(UserMembership.builder().user(user).brand(brand).master(master).build());
        }
    }

    @Test
    @DisplayName("POST /auth/v1/context выставляет подписанную cookie ctx")
    void setContext_setsSignedCookie() throws Exception {
        String body = "{" +
                "\"masterId\":" + master.getId() + "," +
                "\"brandId\":" + brand.getId() + "," +
                "\"pickupPointId\":null" +
                "}";

        var res = mvc.perform(post("/auth/v1/context")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String setCookie = res.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("ctx=");

        // Извлечём значение cookie и проверим подпись HMAC
        String value = setCookie.substring(setCookie.indexOf("ctx=") + 4);
        value = value.split(";", 2)[0];
        String[] parts = value.split("\\.");
        assertThat(parts.length).isEqualTo(2);
        String payloadB64 = parts[0];
        String sig = parts[1];
        HmacSigner signer = new HmacSigner("change-me");
        assertThat(signer.signBase64Url(payloadB64)).isEqualTo(sig);

        // Декодируем payload и убеждаемся, что там наши поля
        String json = new String(Base64.getUrlDecoder().decode(payloadB64));
        assertThat(json).contains("\"masterId\":" + master.getId());
        assertThat(json).contains("\"brandId\":" + brand.getId());
    }

    @Test
    @DisplayName("DELETE /auth/v1/context стирает cookie ctx (maxAge=0)")
    void clearContext_deletesCookie() throws Exception {
        var res = mvc.perform(delete("/auth/v1/context")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();
        String setCookie = res.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("ctx=");
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    @DisplayName("POST /auth/v1/context: override brandId, не принадлежащий пользователю -> 400")
    void setContext_overrideForeignBrand_returns400() throws Exception {
        // Создаём чужой бренд без membership
        Brand foreign = brandRepo.save(Brand.builder().name("b-foreign").organizationName("org").master(master).build());
        String body = "{" +
                "\"masterId\":" + master.getId() + "," +
                "\"brandId\":" + foreign.getId() +
                "}";
        mvc.perform(post("/auth/v1/context")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
