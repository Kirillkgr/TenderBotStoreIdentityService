package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.UpdateGroupTagRequest;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("RBAC: GroupTagController")
@Transactional
@Tag("rbac-grouptag")
class GroupTagControllerRbacIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @Autowired
    MembershipFixtures fx;
    @Autowired
    BrandRepository brandRepository;

    String username;
    Cookie login;

    @BeforeEach
    void setUp() throws Exception {
        username = "rbac-gt-" + System.nanoTime();
        login = fx.registerAndLogin(username);
    }

    @Test
    @DisplayName("ADMIN: create group tag -> 201")
    void admin_can_create_group_tag() throws Exception {
        var ctx = fx.prepareRoleMembershipWithBrand(login, username, RoleMembership.ADMIN);

        CreateGroupTagRequest req = new CreateGroupTagRequest();
        req.setName("GT-Admin-" + System.nanoTime());
        req.setBrandId(resolveBrandId());
        req.setParentId(null);

        mvc.perform(post("/auth/v1/group-tags")
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CLIENT: create group tag -> 403")
    void client_forbidden_create_group_tag() throws Exception {
        var ctx = fx.prepareRoleMembershipWithBrand(login, username, RoleMembership.CLIENT);

        CreateGroupTagRequest req = new CreateGroupTagRequest();
        req.setName("GT-Client-" + System.nanoTime());
        req.setBrandId(resolveBrandId());
        req.setParentId(null);

        mvc.perform(post("/auth/v1/group-tags")
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN: updateFull -> 200; CLIENT -> 403")
    void update_full_requires_owner_or_admin() throws Exception {
        var admin = fx.prepareRoleMembershipWithBrand(login, username, RoleMembership.ADMIN);
        long brandId = resolveBrandId();

        // Create group tag to update
        CreateGroupTagRequest create = new CreateGroupTagRequest();
        create.setName("GT-Upd-" + System.nanoTime());
        create.setBrandId(brandId);
        Long id = om.readTree(mvc.perform(post("/auth/v1/group-tags")
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(create)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // ADMIN updateFull
        UpdateGroupTagRequest upd = new UpdateGroupTagRequest();
        upd.setName("GT-Upd-Rename-" + System.nanoTime());
        upd.setParentId(null);
        upd.setBrandId(brandId);
        mvc.perform(put("/auth/v1/group-tags/{id}/full", id)
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(upd)))
                .andExpect(status().isOk());

        // CLIENT forbidden (use separate user to avoid duplicate membership on same (user, brand))
        String clientUsername1 = username + "-client-" + System.nanoTime();
        Cookie clientLogin1 = fx.registerAndLogin(clientUsername1);
        var client = fx.prepareRoleMembershipWithBrand(clientLogin1, clientUsername1, RoleMembership.CLIENT);
        mvc.perform(put("/auth/v1/group-tags/{id}/full", id)
                        .cookie(client.cookie())
                        .header("Authorization", "Bearer " + client.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(upd)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("READ breadcrumbs requires auth -> 200/401")
    void breadcrumbs_auth_required() throws Exception {
        var admin = fx.prepareRoleMembershipWithBrand(login, username, RoleMembership.ADMIN);
        long brandId = resolveBrandId();

        // Create
        CreateGroupTagRequest create = new CreateGroupTagRequest();
        create.setName("GT-Bc-" + System.nanoTime());
        create.setBrandId(brandId);
        Long id = om.readTree(mvc.perform(post("/auth/v1/group-tags")
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(create)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // Auth ok
        mvc.perform(get("/auth/v1/group-tags/breadcrumbs/{id}", id)
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue()))
                .andExpect(status().isOk());

        // Anonymous 401
        mvc.perform(get("/auth/v1/group-tags/breadcrumbs/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET tree without auth -> 401")
    void anonymous_get_tree_unauthorized() throws Exception {
        mvc.perform(get("/auth/v1/group-tags/tree/{brandId}", resolveBrandId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN: rename group tag -> 200; CLIENT -> 403")
    void rename_requires_owner_or_admin() throws Exception {
        var admin = fx.prepareRoleMembershipWithBrand(login, username, RoleMembership.ADMIN);

        // Create first as ADMIN to obtain id
        long brandId = resolveBrandId();
        CreateGroupTagRequest create = new CreateGroupTagRequest();
        create.setName("GT-Rename-" + System.nanoTime());
        create.setBrandId(brandId);
        var createRes = mvc.perform(post("/auth/v1/group-tags")
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(create)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(createRes.getResponse().getContentAsString());
        Long groupTagId = node.get("id").asLong();
        assertThat(groupTagId).isNotNull();

        // ADMIN can rename
        mvc.perform(put("/auth/v1/group-tags/{id}", groupTagId)
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .param("name", "Renamed-" + System.nanoTime()))
                .andExpect(status().isOk());

        // CLIENT forbidden (use separate user to avoid duplicate membership on same (user, brand))
        String clientUsername2 = username + "-client-" + System.nanoTime();
        Cookie clientLogin2 = fx.registerAndLogin(clientUsername2);
        var client = fx.prepareRoleMembershipWithBrand(clientLogin2, clientUsername2, RoleMembership.CLIENT);
        mvc.perform(put("/auth/v1/group-tags/{id}", groupTagId)
                        .cookie(client.cookie())
                        .header("Authorization", "Bearer " + client.cookie().getValue())
                        .param("name", "X"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN: deleteWithArchive -> 204; CLIENT -> 403")
    void delete_with_archive_requires_owner_or_admin() throws Exception {
        var admin = fx.prepareRoleMembershipWithBrand(login, username, RoleMembership.ADMIN);
        long brandId = resolveBrandId();

        // Create
        CreateGroupTagRequest create = new CreateGroupTagRequest();
        create.setName("GT-Del-" + System.nanoTime());
        create.setBrandId(brandId);
        Long id = om.readTree(mvc.perform(post("/auth/v1/group-tags")
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(create)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // ADMIN can delete (to archive)
        mvc.perform(delete("/auth/v1/group-tags/{id}", id)
                        .cookie(admin.cookie())
                        .header("Authorization", "Bearer " + admin.cookie().getValue()))
                .andExpect(status().isNoContent());

        // CLIENT forbidden on the same id (even if archived record exists)
        String clientUsername3 = username + "-client-" + System.nanoTime();
        Cookie clientLogin3 = fx.registerAndLogin(clientUsername3);
        var client = fx.prepareRoleMembershipWithBrand(clientLogin3, clientUsername3, RoleMembership.CLIENT);
        mvc.perform(delete("/auth/v1/group-tags/{id}", id)
                        .cookie(client.cookie())
                        .header("Authorization", "Bearer " + client.cookie().getValue()))
                .andExpect(status().isForbidden());
    }

    private long resolveBrandId() {
        return brandRepository.findAll().stream()
                .findFirst()
                .map(Brand::getId)
                .orElseThrow(() -> new IllegalStateException("No Brand present for tests"));
    }
}
