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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class StockControllerRbacReadIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    TestFixtures fx;

    @Test
    void anonymous_cannot_read_stock() throws Exception {
        mvc.perform(get("/auth/v1/inventory/stock").param("warehouseId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "client", roles = {"USER"})
    void client_forbidden_on_read_stock() throws Exception {
        long masterId = fx.ensureMaster();
        mvc.perform(get("/auth/v1/inventory/stock")
                        .param("warehouseId", String.valueOf(fx.warehouse("WH-RBAC")))
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            kirillzhdanov.identityservice.tenant.TenantContext.setMasterId(masterId);
                            kirillzhdanov.identityservice.tenant.TenantContext.setRole(RoleMembership.CLIENT);
                            return req;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cook", roles = {"COOK"})
    void cook_can_read_stock() throws Exception {
        long masterId = fx.ensureMaster();
        long w = fx.warehouse("WH-RBAC-COOK");
        mvc.perform(get("/auth/v1/inventory/stock")
                        .param("warehouseId", String.valueOf(w))
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            kirillzhdanov.identityservice.tenant.TenantContext.setMasterId(masterId);
                            kirillzhdanov.identityservice.tenant.TenantContext.setRole(RoleMembership.COOK);
                            return req;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void cashier_can_read_stock() throws Exception {
        long masterId = fx.ensureMaster();
        long w = fx.warehouse("WH-RBAC-CASHIER");
        mvc.perform(get("/auth/v1/inventory/stock")
                        .param("warehouseId", String.valueOf(w))
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            kirillzhdanov.identityservice.tenant.TenantContext.setMasterId(masterId);
                            kirillzhdanov.identityservice.tenant.TenantContext.setRole(RoleMembership.CASHIER);
                            return req;
                        }))
                .andExpect(status().isOk());
    }
}
