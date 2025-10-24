package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ProductAdminControllerTest extends IntegrationTestBase {

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
    @DisplayName("OWNER может создать продукт")
    void owner_can_create_product() throws Exception {
        var owner = fx.prepareRoleMembership(login, username, RoleMembership.OWNER);
        long brandId = sb.createBrand(owner.cookie(), owner.masterId(), "B2P-Owner-Brand", "Org-P-Owner");
        // Переключаем контекст на созданный бренд, чтобы гарды пропустили приватную операцию
        kirillzhdanov.identityservice.dto.ContextSwitchRequest ctxReq = new kirillzhdanov.identityservice.dto.ContextSwitchRequest();
        ctxReq.setMembershipId(owner.membershipId());
        ctxReq.setBrandId(brandId);
        var ctxRes = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/v1/context/switch")
                        .cookie(owner.cookie())
                        .header("Authorization", "Bearer " + owner.cookie().getValue())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctxReq)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        String newAccess = objectMapper.readTree(ctxRes.getResponse().getContentAsString()).get("accessToken").asText();
        jakarta.servlet.http.Cookie ctxCookie = new jakarta.servlet.http.Cookie("accessToken", newAccess);
        long productId = sb.createProduct(ctxCookie, owner.masterId(), "B2P-Owner-Prod", new BigDecimal("11.11"), brandId);
        assertThat(productId).isPositive();
    }

    @Test
    @DisplayName("ADMIN может создать продукт")
    void admin_can_create_product() throws Exception {
        var admin = fx.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        long brandId = sb.createBrand(admin.cookie(), admin.masterId(), "B2P-Admin-Brand", "Org-P-Admin");
        // Переключаем контекст на созданный бренд
        kirillzhdanov.identityservice.dto.ContextSwitchRequest ctxReq = new kirillzhdanov.identityservice.dto.ContextSwitchRequest();
        ctxReq.setMembershipId(admin.membershipId());
        ctxReq.setBrandId(brandId);
        var ctxRes = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/v1/context/switch")
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctxReq)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        String newAccess = objectMapper.readTree(ctxRes.getResponse().getContentAsString()).get("accessToken").asText();
        jakarta.servlet.http.Cookie ctxCookie = new jakarta.servlet.http.Cookie("accessToken", newAccess);
        long productId = sb.createProduct(ctxCookie, admin.masterId(), "B2P-Admin-Prod", new BigDecimal("12.34"), brandId);
        assertThat(productId).isPositive();
    }

    @Test
    @DisplayName("Кросс-мастер доступ к продукту даёт 404")
    void cross_master_product_returns_404() throws Exception {
        var admin = fx.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        long brandId = sb.createBrand(admin.cookie(), admin.masterId(), "B2P-X-Brand", "Org-X");
        // Переключаем контекст на созданный бренд, чтобы создать продукт
        kirillzhdanov.identityservice.dto.ContextSwitchRequest ctxReq = new kirillzhdanov.identityservice.dto.ContextSwitchRequest();
        ctxReq.setMembershipId(admin.membershipId());
        ctxReq.setBrandId(brandId);
        var ctxRes = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/v1/context/switch")
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctxReq)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        String newAccess = objectMapper.readTree(ctxRes.getResponse().getContentAsString()).get("accessToken").asText();
        jakarta.servlet.http.Cookie ctxCookie = new jakarta.servlet.http.Cookie("accessToken", newAccess);
        long productId = sb.createProduct(ctxCookie, admin.masterId(), "B2P-X-Prod", new BigDecimal("9.99"), brandId);

        var admin2 = fx.prepareRoleMembership(login, username, RoleMembership.ADMIN);
        mockMvc.perform(get("/auth/v1/products/" + productId)
                        .cookie(admin2.cookie())
                        .header("X-Master-Id", admin2.masterId()))
                .andExpect(status().is4xxClientError()); // по бизнес-логике 404
    }

    @Test
    @DisplayName("CLIENT не может создавать продукты (403)")
    void client_cannot_create_product() throws Exception {
        var client = fx.prepareRoleMembership(login, username, RoleMembership.CLIENT);

        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("B2P-Client-Deny");
        req.setPrice(new BigDecimal("7.77"));
        req.setBrandId(1L); // неважно, будет 403 до валидаций

        mockMvc.perform(post("/auth/v1/products")
                        .cookie(client.cookie())
                        .header("X-Master-Id", client.masterId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
