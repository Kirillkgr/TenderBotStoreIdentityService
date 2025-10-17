package kirillzhdanov.identityservice.controller.inventory;

import com.jayway.jsonpath.JsonPath;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.tenant.TenantContext;
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
class SupplyControllerIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    kirillzhdanov.identityservice.testutil.TestFixtures fx;

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_and_post_supply_increments_stock() throws Exception {
        long w = fx.warehouse("Склад тест");
        long u = fx.unit("кг");
        long ing = fx.ingredient("ингредиент A", u);
        long masterId = fx.ensureMaster();

        String body = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "notes": "it",
                      "items": [ { "ingredientId": %d, "qty": 2.5 } ]
                    }
                """.formatted(w, ing);

        var res = mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        Number supplyIdNumber = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNumber.longValue();

        mvc.perform(post("/auth/v1/inventory/supplies/{id}/post", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));

        mvc.perform(get("/auth/v1/inventory/stock").param("warehouseId", String.valueOf(w))
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ingredientId").value((int) ing))
                .andExpect(jsonPath("$[0].quantity").value("2.5"));
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_supply_with_invalid_qty_returns_400() throws Exception {
        long w = fx.warehouse("Склад тест 2");
        long u = fx.unit("кг");
        long ing = fx.ingredient("ингредиент B", u);
        long masterId = fx.ensureMaster();

        String body = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 0 } ]
                    }
                """.formatted(w, ing);

        mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymous_cannot_create_supply() throws Exception {
        mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
