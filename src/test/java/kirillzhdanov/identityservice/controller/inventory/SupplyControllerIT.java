package kirillzhdanov.identityservice.controller.inventory;

import com.jayway.jsonpath.JsonPath;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.repository.inventory.StockBatchRepository;
import kirillzhdanov.identityservice.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SupplyControllerIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    kirillzhdanov.identityservice.testutil.TestFixtures fx;
    @Autowired
    StockBatchRepository stockBatchRepository;

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
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void update_with_empty_items_returns_400() throws Exception {
        long w = fx.warehouse("Склад пустые позиции");
        long u = fx.unit("кг");
        long ing = fx.ingredient("ингредиент Z", u);
        long masterId = fx.ensureMaster();

        String createBody = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 1.0, "unitCost": 1 } ]
                    }
                """.formatted(w, ing);

        var res = mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        Number supplyIdNum = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNum.longValue();

        String updateBody = """
                    {
                      "items": []
                    }
                """;

        mvc.perform(put("/auth/v1/inventory/supplies/{id}", supplyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_supply_without_unit_cost_sets_total_cost_null() throws Exception {
        long w = fx.warehouse("Склад без цены");
        long u = fx.unit("шт");
        long ing = fx.ingredient("ингредиент G", u);
        long masterId = fx.ensureMaster();

        String body = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 3 } ]
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

        Number supplyIdNum = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNum.longValue();

        mvc.perform(get("/auth/v1/inventory/supplies/{id}", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").doesNotExist());
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_supply_multiple_items_total_cost_rounding() throws Exception {
        long w = fx.warehouse("Склад округление");
        long u = fx.unit("кг");
        long ing1 = fx.ingredient("ингредиент H1", u);
        long ing2 = fx.ingredient("ингредиент H2", u);
        long masterId = fx.ensureMaster();

        // totalCost expected = 2.5*10.123456 + 1*0.1 = 25.30864 + 0.1 = 25.40864 (scale=6)
        String body = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [
                        { "ingredientId": %d, "qty": 2.5, "unitCost": 10.123456 },
                        { "ingredientId": %d, "qty": 1,   "unitCost": 0.1 }
                      ]
                    }
                """.formatted(w, ing1, ing2);

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

        Number supplyIdNum = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNum.longValue();

        mvc.perform(get("/auth/v1/inventory/supplies/{id}", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(25.40864));
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void receive_directly_from_draft_creates_batches_and_sets_received() throws Exception {
        long w = fx.warehouse("Склад direct receive");
        long u = fx.unit("шт");
        long ing = fx.ingredient("ингредиент R", u);
        long masterId = fx.ensureMaster();

        String body = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 4, "unitCost": 2.5 } ]
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

        Number supplyIdNum = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNum.longValue();

        mvc.perform(post("/auth/v1/inventory/supplies/{id}/receive", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        var batches = stockBatchRepository.findAllByMaster_IdAndIngredient_Id(masterId, ing);
        org.assertj.core.api.Assertions.assertThat(batches)
                .isNotEmpty()
                .anySatisfy(b -> org.assertj.core.api.Assertions.assertThat(b.getUnitCost()).isNotNull().isEqualByComparingTo("2.5"));
    }

    @Test
    void anonymous_cannot_create_supply() throws Exception {
        mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_supply_with_unit_cost_computes_total_cost() throws Exception {
        long w = fx.warehouse("Склад ценообразование");
        long u = fx.unit("кг");
        long ing = fx.ingredient("ингредиент C", u);
        long masterId = fx.ensureMaster();

        String body = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 2.5, "unitCost": 10.123456 } ]
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

        mvc.perform(get("/auth/v1/inventory/supplies/{id}", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                // 2.5 * 10.123456 = 25.30864
                .andExpect(jsonPath("$.totalCost").value(25.30864))
                .andExpect(jsonPath("$.items[0].unitCost").value(10.123456));
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_supply_with_negative_unit_cost_returns_400() throws Exception {
        long w = fx.warehouse("Склад цены негатив");
        long u = fx.unit("шт");
        long ing = fx.ingredient("ингредиент D", u);
        long masterId = fx.ensureMaster();

        String body = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 1, "unitCost": -1 } ]
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
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void approve_locks_total_cost_and_receive_creates_batches_with_unit_cost() throws Exception {
        long w = fx.warehouse("Склад approve/receive");
        long u = fx.unit("кг");
        long ing = fx.ingredient("ингредиент E", u);
        long masterId = fx.ensureMaster();

        String createBody = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 3.0, "unitCost": 9.5 } ]
                    }
                """.formatted(w, ing);

        var res = mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        Number supplyIdNumber2 = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNumber2.longValue();

        // Approve
        mvc.perform(post("/auth/v1/inventory/supplies/{id}/approve", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Receive
        mvc.perform(post("/auth/v1/inventory/supplies/{id}/receive", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        // Verify batch created with unitCost via repository
        var batches = stockBatchRepository.findAllByMaster_IdAndIngredient_Id(masterId, ing);
        org.assertj.core.api.Assertions.assertThat(batches)
                .isNotEmpty()
                .anySatisfy(b -> org.assertj.core.api.Assertions.assertThat(b.getUnitCost()).isNotNull().isEqualByComparingTo("9.5"));
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void update_in_draft_recalculates_total_cost() throws Exception {
        long w = fx.warehouse("Склад update draft");
        long u = fx.unit("кг");
        long ing = fx.ingredient("ингредиент U", u);
        long masterId = fx.ensureMaster();

        // Create DRAFT with totalCost = 1 * 5 = 5
        String createBody = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 1.0, "unitCost": 5 } ]
                    }
                """.formatted(w, ing);

        var res = mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        Number supplyIdNum = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNum.longValue();

        // Update items to qty 2, unitCost 7 => totalCost = 14
        String updateBody = """
                    {
                      "items": [ { "ingredientId": %d, "qty": 2.0, "unitCost": 7 } ]
                    }
                """.formatted(ing);

        mvc.perform(put("/auth/v1/inventory/supplies/{id}", supplyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(14.0))
                .andExpect(jsonPath("$.items[0].unitCost").value(7.0));
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void update_not_allowed_when_not_draft() throws Exception {
        long w = fx.warehouse("Склад update forbid");
        long u = fx.unit("кг");
        long ing = fx.ingredient("ингредиент F", u);
        long masterId = fx.ensureMaster();

        String createBody = """
                    {
                      "warehouseId": %d,
                      "date": "2025-10-11T10:00:00Z",
                      "items": [ { "ingredientId": %d, "qty": 1.0, "unitCost": 1 } ]
                    }
                """.formatted(w, ing);

        var res = mvc.perform(post("/auth/v1/inventory/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        Number supplyIdNum3 = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        long supplyId = supplyIdNum3.longValue();

        // Move to APPROVED
        mvc.perform(post("/auth/v1/inventory/supplies/{id}/approve", supplyId)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Try update -> 400
        String updateBody = """
                    {
                      "items": [ { "ingredientId": %d, "qty": 2.0 } ]
                    }
                """.formatted(ing);

        mvc.perform(put("/auth/v1/inventory/supplies/{id}", supplyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isBadRequest());
    }
}
