package kirillzhdanov.identityservice.controller.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.ContextSwitchRequest;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.dto.inventory.CreateIngredientRequest;
import kirillzhdanov.identityservice.dto.inventory.IngredientDto;
import kirillzhdanov.identityservice.dto.inventory.UpdateIngredientRequest;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.inventory.Unit;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.inventory.UnitRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.testutil.CtxTestCookies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IngredientControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MasterAccountRepository masterRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserMembershipRepository membershipRepo;

    @Autowired
    private UnitRepository unitRepository;

    private Cookie authCookie;
    private String currentUsername;

    @BeforeEach
    void setup() throws Exception {
        authCookie = registerAndLogin();
    }

    private Cookie registerAndLogin() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        String suffix = String.valueOf(System.nanoTime());
        currentUsername = "ingr-user-" + suffix;
        req.setUsername(currentUsername);
        req.setEmail(currentUsername + "@test.local");
        req.setPassword("Password123!");
        Set<Role.RoleName> roles = new HashSet<>();
        roles.add(Role.RoleName.USER);
        req.setRoleNames(roles);

        MvcResult res = mockMvc
                .perform(post("/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        UserResponse created = objectMapper.readValue(res.getResponse().getContentAsString(), UserResponse.class);
        assertThat(created.getAccessToken()).isNotBlank();
        return new Cookie("accessToken", created.getAccessToken());
    }

    private Cookie switchContext(Cookie auth, Long membershipId) throws Exception {
        ContextSwitchRequest req = new ContextSwitchRequest();
        req.setMembershipId(membershipId);
        MvcResult sw = mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(sw.getResponse().getContentAsString()).get("accessToken").asText();
        return new Cookie("accessToken", accessToken);
    }

    private Unit createUnitForMaster(MasterAccount master, String name, String shortName) {
        Unit u = new Unit();
        u.setMaster(master);
        u.setName(name);
        u.setShortName(shortName);
        return unitRepository.save(u);
    }

    @Test
    @DisplayName("Список ингредиентов требует авторизации (401 без токена)")
    void list_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/auth/v1/inventory/ingredients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Создание и листинг ингредиентов по master")
    void createAndList_ScopedByMaster() throws Exception {
        // Arrange: два мастера и ADMIN membership в M1
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();

        // ADMIN membership в M1
        UserMembership um1 = new UserMembership();
        um1.setUser(userRepo.findById(userId).orElseThrow());
        um1.setMaster(m1);
        um1.setRole(RoleMembership.ADMIN);
        Long mem1 = membershipRepo.save(um1).getId();
        Cookie adminCtxM1 = switchContext(authCookie, mem1);
        Cookie ctxM1 = CtxTestCookies.createCtx(m1.getId(), null, null, "change-me");

        // ADMIN membership в M2
        UserMembership um2 = new UserMembership();
        um2.setUser(userRepo.findById(userId).orElseThrow());
        um2.setMaster(m2);
        um2.setRole(RoleMembership.ADMIN);
        Long mem2 = membershipRepo.save(um2).getId();
        Cookie adminCtxM2 = switchContext(authCookie, mem2);
        Cookie ctxM2 = CtxTestCookies.createCtx(m2.getId(), null, null, "change-me");

        // Для каждого master создаём Unit
        Unit u1 = createUnitForMaster(m1, "Килограмм", "кг");
        Unit u2 = createUnitForMaster(m2, "Литр", "л");

        // Act: создаём ингредиент в M1
        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName("Сахар");
        req.setUnitId(u1.getId());
        req.setPackageSize(new BigDecimal("1.000"));
        req.setNotes("Белый");

        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtxM1, ctxM1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Сахар"))
                .andExpect(jsonPath("$.unitId").value(u1.getId()));

        // Assert: в M1 видим 1, в M2 — 0
        MvcResult listM1 = mockMvc.perform(get("/auth/v1/inventory/ingredients")
                        .cookie(adminCtxM1, ctxM1))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m1Items = objectMapper.readValue(listM1.getResponse().getContentAsString(), List.class);
        assertThat(m1Items).hasSize(1);

        MvcResult listM2 = mockMvc.perform(get("/auth/v1/inventory/ingredients")
                        .cookie(adminCtxM2, ctxM2))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m2Items = objectMapper.readValue(listM2.getResponse().getContentAsString(), List.class);
        assertThat(m2Items).isEmpty();
    }

    @Test
    @DisplayName("FK unitId: 400 если юнит не в текущем master")
    void create_InvalidUnit_FK_400() throws Exception {
        // Arrange: один master и ADMIN membership
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();

        UserMembership um1 = new UserMembership();
        um1.setUser(userRepo.findById(userId).orElseThrow());
        um1.setMaster(m1);
        um1.setRole(RoleMembership.ADMIN);
        Long mem1 = membershipRepo.save(um1).getId();
        Cookie adminCtxM1 = switchContext(authCookie, mem1);
        Cookie ctxM1 = CtxTestCookies.createCtx(m1.getId(), null, null, "change-me");

        // Юнит создаём в другом master (m2)
        Unit uOther = createUnitForMaster(m2, "Литр", "л");

        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName("Молоко");
        req.setUnitId(uOther.getId());
        req.setPackageSize(new BigDecimal("1.000"));

        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtxM1, ctxM1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Дубликат имени в рамках одного master -> 409")
    void create_DuplicateNameSameMaster_409() throws Exception {
        // Arrange: мастер и ADMIN membership
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership admin = new UserMembership();
        admin.setUser(userRepo.findById(userId).orElseThrow());
        admin.setMaster(m);
        admin.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(admin).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        Unit u = createUnitForMaster(m, "Штук", "шт");

        CreateIngredientRequest first = new CreateIngredientRequest();
        first.setName("Яйцо");
        first.setUnitId(u.getId());
        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        CreateIngredientRequest dup = new CreateIngredientRequest();
        dup.setName("яЙцО"); // case-insensitive
        dup.setUnitId(u.getId());
        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Одинаковое имя в разных мастерах допускается")
    void create_SameNameDifferentMasters_Allowed() throws Exception {
        // Arrange
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();

        // Admin membership m1
        UserMembership um1 = new UserMembership();
        um1.setUser(userRepo.findById(userId).orElseThrow());
        um1.setMaster(m1);
        um1.setRole(RoleMembership.ADMIN);
        Long mem1 = membershipRepo.save(um1).getId();
        Cookie adminCtxM1 = switchContext(authCookie, mem1);
        Cookie ctxM1 = CtxTestCookies.createCtx(m1.getId(), null, null, "change-me");

        // Admin membership m2
        UserMembership um2 = new UserMembership();
        um2.setUser(userRepo.findById(userId).orElseThrow());
        um2.setMaster(m2);
        um2.setRole(RoleMembership.ADMIN);
        Long mem2 = membershipRepo.save(um2).getId();
        Cookie adminCtxM2 = switchContext(authCookie, mem2);
        Cookie ctxM2 = CtxTestCookies.createCtx(m2.getId(), null, null, "change-me");

        Unit u1 = createUnitForMaster(m1, "Килограмм", "кг");
        Unit u2 = createUnitForMaster(m2, "Килограмм", "кг");

        CreateIngredientRequest dto = new CreateIngredientRequest();
        dto.setName("Соль");
        dto.setUnitId(u1.getId());
        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtxM1, ctxM1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        dto.setUnitId(u2.getId());
        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtxM2, ctxM2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Обновление и удаление ингредиента (OWNER/ADMIN)")
    void updateAndDelete_Admin() throws Exception {
        // Arrange: мастер и ADMIN
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership admin = new UserMembership();
        admin.setUser(userRepo.findById(userId).orElseThrow());
        admin.setMaster(m);
        admin.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(admin).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        Unit u = createUnitForMaster(m, "Литр", "л");

        CreateIngredientRequest create = new CreateIngredientRequest();
        create.setName("Молоко");
        create.setUnitId(u.getId());
        MvcResult createdRes = mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();
        IngredientDto created = objectMapper.readValue(createdRes.getResponse().getContentAsString(), IngredientDto.class);

        UpdateIngredientRequest upd = new UpdateIngredientRequest();
        upd.setName("Молоко ультрапастеризованное");
        upd.setUnitId(u.getId());
        upd.setPackageSize(new BigDecimal("1.0"));
        upd.setNotes("UP");
        mockMvc.perform(put("/auth/v1/inventory/ingredients/" + created.getId())
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Молоко ультрапастеризованное"));

        mockMvc.perform(delete("/auth/v1/inventory/ingredients/" + created.getId())
                        .cookie(adminCtx, ctx))
                .andExpect(status().isNoContent());

        MvcResult list = mockMvc.perform(get("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx))
                .andExpect(status().isOk())
                .andReturn();
        List<?> items = objectMapper.readValue(list.getResponse().getContentAsString(), List.class);
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("RBAC: USER без прав получает 403 на POST/PUT/DELETE")
    void rbac_UserForbiddenOnMutations_403() throws Exception {
        // Arrange
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();

        // Admin membership to create unit and initial ingredient
        UserMembership admin = new UserMembership();
        admin.setUser(userRepo.findById(userId).orElseThrow());
        admin.setMaster(m);
        admin.setRole(RoleMembership.ADMIN);
        Long memAdmin = membershipRepo.save(admin).getId();
        Cookie adminCtx = switchContext(authCookie, memAdmin);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        Unit u = createUnitForMaster(m, "Штук", "шт");
        CreateIngredientRequest create = new CreateIngredientRequest();
        create.setName("Яйцо");
        create.setUnitId(u.getId());
        MvcResult createdRes = mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();
        IngredientDto created = objectMapper.readValue(createdRes.getResponse().getContentAsString(), IngredientDto.class);

        // Switch to CLIENT membership
        UserMembership client = new UserMembership();
        client.setUser(userRepo.findById(userId).orElseThrow());
        client.setMaster(m);
        client.setRole(RoleMembership.CLIENT);
        Long memClient = membershipRepo.save(client).getId();
        Cookie clientCtx = switchContext(authCookie, memClient);

        // POST forbidden
        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(clientCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isForbidden());

        // PUT forbidden
        UpdateIngredientRequest upd = new UpdateIngredientRequest();
        upd.setName("Яйцо куриное");
        upd.setUnitId(u.getId());
        mockMvc.perform(put("/auth/v1/inventory/ingredients/" + created.getId())
                        .cookie(clientCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isForbidden());

        // DELETE forbidden
        mockMvc.perform(delete("/auth/v1/inventory/ingredients/" + created.getId())
                        .cookie(clientCtx, ctx))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Валидация: packageSize < 0 -> 400")
    void validation_NegativePackageSize_400() throws Exception {
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership admin = new UserMembership();
        admin.setUser(userRepo.findById(userId).orElseThrow());
        admin.setMaster(m);
        admin.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(admin).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        Unit u = createUnitForMaster(m, "Килограмм", "кг");
        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName("Сахар");
        req.setUnitId(u.getId());
        req.setPackageSize(new BigDecimal("-1.0"));

        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Валидация: отсутствует unitId -> 400")
    void validation_MissingUnitId_400() throws Exception {
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership admin = new UserMembership();
        admin.setUser(userRepo.findById(userId).orElseThrow());
        admin.setMaster(m);
        admin.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(admin).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName("Сахар");
        // unitId не устанавливаем

        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Валидация: пустое имя -> 400")
    void validation_EmptyName_400() throws Exception {
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership admin = new UserMembership();
        admin.setUser(userRepo.findById(userId).orElseThrow());
        admin.setMaster(m);
        admin.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(admin).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        Unit u = createUnitForMaster(m, "Килограмм", "кг");
        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName("   ");
        req.setUnitId(u.getId());

        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Валидация: слишком длинные name/notes -> 400")
    void validation_TooLongFields_400() throws Exception {
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership admin = new UserMembership();
        admin.setUser(userRepo.findById(userId).orElseThrow());
        admin.setMaster(m);
        admin.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(admin).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        Unit u = createUnitForMaster(m, "Килограмм", "кг");
        String longName = "N".repeat(300);
        String longNotes = "X".repeat(2000);
        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName(longName);
        req.setUnitId(u.getId());
        req.setNotes(longNotes);

        mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404: update/delete ингредиента из другого master")
    void updateDelete_OtherMaster_404() throws Exception {
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();

        // Admin in M1
        UserMembership um1 = new UserMembership();
        um1.setUser(userRepo.findById(userId).orElseThrow());
        um1.setMaster(m1);
        um1.setRole(RoleMembership.ADMIN);
        Long mem1 = membershipRepo.save(um1).getId();
        Cookie adminCtxM1 = switchContext(authCookie, mem1);
        Cookie ctxM1 = CtxTestCookies.createCtx(m1.getId(), null, null, "change-me");

        // Admin in M2
        UserMembership um2 = new UserMembership();
        um2.setUser(userRepo.findById(userId).orElseThrow());
        um2.setMaster(m2);
        um2.setRole(RoleMembership.ADMIN);
        Long mem2 = membershipRepo.save(um2).getId();
        Cookie adminCtxM2 = switchContext(authCookie, mem2);
        Cookie ctxM2 = CtxTestCookies.createCtx(m2.getId(), null, null, "change-me");

        // Create in M1
        Unit u1 = createUnitForMaster(m1, "Килограмм", "кг");
        CreateIngredientRequest create = new CreateIngredientRequest();
        create.setName("Сахар");
        create.setUnitId(u1.getId());
        MvcResult createdRes = mockMvc.perform(post("/auth/v1/inventory/ingredients")
                        .cookie(adminCtxM1, ctxM1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();
        IngredientDto created = objectMapper.readValue(createdRes.getResponse().getContentAsString(), IngredientDto.class);

        // Try update from M2
        UpdateIngredientRequest upd = new UpdateIngredientRequest();
        upd.setName("Сахар-песок");
        upd.setUnitId(createUnitForMaster(m2, "Литр", "л").getId());
        mockMvc.perform(put("/auth/v1/inventory/ingredients/" + created.getId())
                        .cookie(adminCtxM2, ctxM2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isNotFound());

        // Try delete from M2
        mockMvc.perform(delete("/auth/v1/inventory/ingredients/" + created.getId())
                        .cookie(adminCtxM2, ctxM2))
                .andExpect(status().isNotFound());
    }
}
