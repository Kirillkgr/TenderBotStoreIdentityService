package kirillzhdanov.identityservice.controller.inventory;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.testutil.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class SupplyControllerRbacReadIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    TestFixtures fx;

    @Test
    void anonymous_cannot_search_supplies() throws Exception {
        mvc.perform(post("/auth/v1/inventory/supplies/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "client", roles = {"USER"})
    void client_forbidden_on_search_supplies() throws Exception {
        long masterId = fx.ensureMaster();
        mvc.perform(post("/auth/v1/inventory/supplies/search")
                        .with(req -> {
                            kirillzhdanov.identityservice.tenant.TenantContext.setMasterId(masterId);
                            kirillzhdanov.identityservice.tenant.TenantContext.setRole(RoleMembership.CLIENT);
                            return req;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cook", roles = {"COOK"})
    void cook_can_search_supplies() throws Exception {
        long masterId = fx.ensureMaster();
        mvc.perform(post("/auth/v1/inventory/supplies/search")
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            kirillzhdanov.identityservice.tenant.TenantContext.setMasterId(masterId);
                            kirillzhdanov.identityservice.tenant.TenantContext.setRole(RoleMembership.COOK);
                            return req;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void anonymous_cannot_get_supply() throws Exception {
        mvc.perform(get("/auth/v1/inventory/supplies/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "client", roles = {"USER"})
    void client_forbidden_on_get_supply() throws Exception {
        long masterId = fx.ensureMaster();
        mvc.perform(get("/auth/v1/inventory/supplies/{id}", 1L)
                        .with(req -> {
                            kirillzhdanov.identityservice.tenant.TenantContext.setMasterId(masterId);
                            kirillzhdanov.identityservice.tenant.TenantContext.setRole(RoleMembership.CLIENT);
                            return req;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void cashier_gets_404_when_supply_absent_but_has_access() throws Exception {
        long masterId = fx.ensureMaster();
        mvc.perform(get("/auth/v1/inventory/supplies/{id}", 1L)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            kirillzhdanov.identityservice.tenant.TenantContext.setMasterId(masterId);
                            kirillzhdanov.identityservice.tenant.TenantContext.setRole(RoleMembership.CASHIER);
                            return req;
                        }))
                .andExpect(status().isNotFound());
    }
}
