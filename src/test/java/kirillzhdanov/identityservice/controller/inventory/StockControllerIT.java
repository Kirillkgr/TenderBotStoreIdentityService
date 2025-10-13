package kirillzhdanov.identityservice.controller.inventory;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.tenant.TenantContext;
import kirillzhdanov.identityservice.testutil.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class StockControllerIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    TestFixtures fx;

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void increase_and_decrease_stock_updates_qty_and_prevents_negative() throws Exception {
        long u = fx.unit("кг");
        long w = fx.warehouse("Склад-1");
        long ing = fx.ingredient("Сахар", u);
        long masterId = fx.ensureMaster();

        // Increase 2.5
        String incBody = """
                    { "ingredientId": %d, "warehouseId": %d, "qty": 2.5 }
                """.formatted(ing, w);
        mvc.perform(post("/auth/v1/inventory/stock/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incBody)
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value("2.5"));

        // List by filters (ingredient + warehouse)
        mvc.perform(get("/auth/v1/inventory/stock")
                        .param("ingredientId", String.valueOf(ing))
                        .param("warehouseId", String.valueOf(w))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value("2.5"));

        // Decrease 1.0
        String decBody = """
                    { "ingredientId": %d, "warehouseId": %d, "qty": 1.0 }
                """.formatted(ing, w);
        mvc.perform(post("/auth/v1/inventory/stock/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decBody)
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value("1.5"));

        // Negative prevention: decrease 5.0 should be 400
        String badDecBody = """
                    { "ingredientId": %d, "warehouseId": %d, "qty": 5.0 }
                """.formatted(ing, w);
        mvc.perform(post("/auth/v1/inventory/stock/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badDecBody)
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "cook", roles = {"COOK"})
    void cook_cannot_mutate_stock() throws Exception {
        long u = fx.unit("кг");
        long w = fx.warehouse("Склад-2");
        long ing = fx.ingredient("Соль", u);
        long masterId = fx.ensureMaster();

        String body = """
                    { "ingredientId": %d, "warehouseId": %d, "qty": 1.0 }
                """.formatted(ing, w);
        mvc.perform(post("/auth/v1/inventory/stock/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.COOK);
                            return req;
                        }))
                .andExpect(status().isForbidden());
    }
}
