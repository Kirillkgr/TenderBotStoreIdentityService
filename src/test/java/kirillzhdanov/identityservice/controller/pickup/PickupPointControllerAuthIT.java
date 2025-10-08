package kirillzhdanov.identityservice.controller.pickup;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Auth: PickupPointController")
@Tag("pickup-auth")
class PickupPointControllerAuthIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    MembershipFixtures fx;
    @Autowired
    BrandRepository brandRepository;

    @Test
    @DisplayName("GET /brand/{brandId}/pickup-points -> 401 без аутентификации")
    void list_unauthorized_without_auth() throws Exception {
        long brandId = resolveBrandId();
        mvc.perform(get("/brand/{brandId}/pickup-points", brandId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /brand/{brandId}/pickup-points -> 200 c аутентификацией")
    void list_ok_with_auth() throws Exception {
        String username = "pickup-auth-" + System.nanoTime();
        Cookie login = fx.registerAndLogin(username);
        var ctx = fx.prepareRoleMembershipWithBrand(login, username, RoleMembership.CLIENT);

        long brandId = resolveBrandId();
        mvc.perform(get("/brand/{brandId}/pickup-points", brandId)
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue()))
                .andExpect(status().isOk());
    }

    private long resolveBrandId() {
        return brandRepository.findAll().stream()
                .findFirst()
                .map(Brand::getId)
                .orElseThrow(() -> new IllegalStateException("No Brand present for tests"));
    }
}
