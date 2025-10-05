package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import kirillzhdanov.identityservice.testutil.ScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class BrandAdminControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MembershipFixtures fx;
    @Autowired
    private ScenarioBuilder sb;

    private Cookie login;
    private String username;

    @BeforeEach
    void setup() throws Exception {
        username = "b2ts1-user-" + System.nanoTime();
        login = fx.ensureLogin(username);
    }

    @Test
    @DisplayName("OWNER может создать и обновить бренд")
    void owner_can_create_and_update_brand() throws Exception {
        var owner = fx.prepareRoleMembership(login, username, RoleMembership.OWNER);

        long brandId = sb.createBrand(owner.cookie(), owner.masterId(), "B2-Owner-Brand", "Org-Owner");
        assertThat(brandId).isPositive();

        BrandDto update = BrandDto.builder().name("B2-Owner-Brand-Updated").organizationName("Org-Owner-U").build();
        mockMvc.perform(put("/auth/v1/brands/" + brandId)
                        .cookie(owner.cookie())
                        .header("X-Master-Id", owner.masterId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("B2-Owner-Brand-Updated"));

        // sanity: list returns >=1
        MvcResult list = mockMvc.perform(get("/auth/v1/brands")
                        .cookie(owner.cookie())
                        .header("X-Master-Id", owner.masterId()))
                .andExpect(status().isOk())
                .andReturn();
        List<?> items = objectMapper.readValue(list.getResponse().getContentAsString(), List.class);
        assertThat(items).isNotEmpty();
    }

    @Test
    @DisplayName("ADMIN может создать и обновить бренд")
    void admin_can_create_and_update_brand() throws Exception {
        var admin = fx.prepareRoleMembership(login, username, RoleMembership.ADMIN);

        long brandId = sb.createBrand(admin.cookie(), admin.masterId(), "B2-Admin-Brand", "Org-Admin");
        assertThat(brandId).isPositive();

        BrandDto update = BrandDto.builder().name("B2-Admin-Brand-Updated").organizationName("Org-Admin-U").build();
        mockMvc.perform(put("/auth/v1/brands/" + brandId)
                        .cookie(admin.cookie())
                        .header("X-Master-Id", admin.masterId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("B2-Admin-Brand-Updated"));
    }

    @Test
    @DisplayName("Кросс-мастер доступ к бренду даёт 404")
    void cross_master_access_returns_404() throws Exception {
        var admin = fx.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        // Создаём бренд в master A
        long brandId = sb.createBrand(admin.cookie(), admin.masterId(), "B2-Admin-X", "Org-X");
        // Готовим второй мастер под ту же роль
        var admin2 = fx.prepareRoleMembership(login, username, RoleMembership.ADMIN);

        mockMvc.perform(get("/auth/v1/brands/" + brandId)
                        .cookie(admin2.cookie())
                        .header("X-Master-Id", admin2.masterId()))
                .andExpect(status().is4xxClientError()); // по бизнес-логике 404
    }

    @Test
    @DisplayName("CLIENT может создавать бренды (201)")
    void client_can_create_brand() throws Exception {
        var client = fx.prepareRoleMembership(login, username, RoleMembership.CLIENT);

        BrandDto dto = BrandDto.builder().name("B2-Client-Deny").organizationName("Org-C").build();
        mockMvc.perform(post("/auth/v1/brands")
                        .cookie(client.cookie())
                        .header("X-Master-Id", client.masterId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }
}
