package kirillzhdanov.identityservice.controller.inventory;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PackagingControllerIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    kirillzhdanov.identityservice.testutil.TestFixtures fx;

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void packaging_crud_works() throws Exception {
        long u = fx.unit("кг");
        long masterId = fx.ensureMaster();

        // create
        String createBody = """
                    { "name":"Пакет", "unitId": %d, "size": 1.0 }
                """.formatted(u);
        var res = mvc.perform(post("/auth/v1/inventory/packagings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Пакет"))
                .andReturn();

        int id = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");

        // list
        mvc.perform(get("/auth/v1/inventory/packagings")
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        // update
        String updateBody = "{ \"name\": \"Пакет большой\" }";
        mvc.perform(put("/auth/v1/inventory/packagings/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Пакет большой"));

        // delete
        mvc.perform(delete("/auth/v1/inventory/packagings/{id}", id)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isNoContent());

        // not found after delete
        mvc.perform(put("/auth/v1/inventory/packagings/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .cookie(kirillzhdanov.identityservice.testutil.CtxTestCookies.createCtx(masterId, null, null, "change-me"))
                        .with(req -> {
                            TenantContext.setMasterId(masterId);
                            TenantContext.setRole(RoleMembership.OWNER);
                            return req;
                        }))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "cook", roles = {"COOK"})
    void cook_cannot_mutate_packaging() throws Exception {
        long u = fx.unit("кг");
        String createBody = """
                    { "name":"Пакет", "unitId": %d, "size": 1.0 }
                """.formatted(u);
        mvc.perform(post("/auth/v1/inventory/packagings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymous_cannot_access_packaging_list() throws Exception {
        mvc.perform(get("/auth/v1/inventory/packagings"))
                .andExpect(status().isUnauthorized());
    }
}
