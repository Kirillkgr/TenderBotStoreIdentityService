package kirillzhdanov.identityservice.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class MembershipFixturesTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MembershipFixtures fixtures;
    @Autowired
    private ScenarioBuilder sb;

    private Cookie login;

    @BeforeEach
    void setup() throws Exception {
        login = fixtures.ensureLogin("fx-user");
    }

    @Test
    @DisplayName("prepareAllRoleMemberships: создаёт контексты для всех ролей и позволяет читать список брендов")
    void prepareAllRoles_basic() throws Exception {
        Map<RoleMembership, MembershipFixtures.Context> ctxs = fixtures.prepareAllRoleMemberships(login, "fx-user");
        assertThat(ctxs).containsKeys(RoleMembership.values());

        // любой контекст должен иметь возможность читать GET бренды (публичная операция)
        for (MembershipFixtures.Context c : ctxs.values()) {
            mockMvc.perform(get("/auth/v1/brands")
                            .cookie(c.cookie())
                            .header("X-Master-Id", c.masterId()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("prepareRoleMembership + ScenarioBuilder: создаёт бренд и продукт в ADMIN-контексте")
    void adminCanCreateBrandAndProduct() throws Exception {
        var admin = fixtures.prepareRoleMembership(login, "fx-user", RoleMembership.ADMIN);
        long brandId = sb.createBrand(admin.cookie(), admin.masterId(), "FX-BRAND", "FX-ORG");
        long productId = sb.createProduct(admin.cookie(), "FX-PROD", new BigDecimal("9.99"), brandId);
        assertThat(brandId).isPositive();
        assertThat(productId).isPositive();
    }

    @Test
    @DisplayName("Кеширование ensureLogin: повторный вызов возвращает тот же токен (пока не switchContext)")
    void ensureLoginCachesToken() throws Exception {
        Cookie c1 = fixtures.ensureLogin("fx-user");
        Cookie c2 = fixtures.ensureLogin("fx-user");
        assertThat(c1.getValue()).isEqualTo(c2.getValue());
    }
}
