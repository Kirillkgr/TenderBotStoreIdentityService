package kirillzhdanov.identityservice.controller.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.ContextSwitchRequest;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.dto.inventory.SupplierDto;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.testutil.CtxTestCookies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SupplierControllerIntegrationTest extends IntegrationTestBase {

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

    private Cookie authCookie;
    private String currentUsername;

    @BeforeEach
    void setup() throws Exception {
        authCookie = registerAndLogin();
    }

    private Cookie registerAndLogin() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        String suffix = String.valueOf(System.nanoTime());
        currentUsername = "supplier-user-" + suffix;
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

    @Test
    @DisplayName("Создание и листинг поставщиков изолированы по master")
    void createAndList_ScopedByMaster() throws Exception {
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership um1 = new UserMembership();
        um1.setUser(userRepo.findById(userId).orElseThrow());
        um1.setMaster(m1);
        um1.setRole(RoleMembership.ADMIN);
        Long mem1 = membershipRepo.save(um1).getId();
        Cookie adminCtx = switchContext(authCookie, mem1);
        Cookie ctxM1 = CtxTestCookies.createCtx(m1.getId(), null, null, "change-me");
        Cookie ctxM2 = CtxTestCookies.createCtx(m2.getId(), null, null, "change-me");

        SupplierDto dto = new SupplierDto(null, "Поставщик А", "+79990000000", "a@example.com", "Адрес 1");
        mockMvc.perform(post("/auth/v1/inventory/suppliers")
                        .cookie(adminCtx, ctxM1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Поставщик А"));

        MvcResult listM1 = mockMvc.perform(get("/auth/v1/inventory/suppliers")
                        .cookie(adminCtx, ctxM1))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m1Items = objectMapper.readValue(listM1.getResponse().getContentAsString(), List.class);
        assertThat(m1Items).hasSize(1);

        MvcResult listM2 = mockMvc.perform(get("/auth/v1/inventory/suppliers")
                        .cookie(adminCtx, ctxM2))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m2Items = objectMapper.readValue(listM2.getResponse().getContentAsString(), List.class);
        assertThat(m2Items).isEmpty();
    }

    @Test
    @DisplayName("Обновление и удаление поставщика (OWNER/ADMIN)")
    void updateAndDelete_Admin() throws Exception {
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership um = new UserMembership();
        um.setUser(userRepo.findById(userId).orElseThrow());
        um.setMaster(m);
        um.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(um).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        SupplierDto dto = new SupplierDto(null, "Поставщик X", null, null, null);
        MvcResult createdRes = mockMvc.perform(post("/auth/v1/inventory/suppliers")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        SupplierDto created = objectMapper.readValue(createdRes.getResponse().getContentAsString(), SupplierDto.class);

        SupplierDto upd = new SupplierDto(created.getId(), "Поставщик Y", "+70000000001", "y@example.com", "Адрес 2");
        mockMvc.perform(put("/auth/v1/inventory/suppliers/" + created.getId())
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Поставщик Y"));

        mockMvc.perform(delete("/auth/v1/inventory/suppliers/" + created.getId())
                        .cookie(adminCtx, ctx))
                .andExpect(status().isNoContent());

        MvcResult list = mockMvc.perform(get("/auth/v1/inventory/suppliers")
                        .cookie(adminCtx, ctx))
                .andExpect(status().isOk())
                .andReturn();
        List<?> items = objectMapper.readValue(list.getResponse().getContentAsString(), List.class);
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("RBAC: POST/PUT/DELETE запрещены для не ADMIN/OWNER (403)")
    void rbac_Forbidden_ForNonAdminOwner() throws Exception {
        // Arrange: создаём мастера и membership с ролью CASHIER
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership um = new UserMembership();
        um.setUser(userRepo.findById(userId).orElseThrow());
        um.setMaster(m);
        um.setRole(RoleMembership.CASHIER);
        Long mem = membershipRepo.save(um).getId();
        Cookie cashierCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        // Try create -> 403
        SupplierDto dto = new SupplierDto(null, "Forbidden S", null, null, null);
        mockMvc.perform(post("/auth/v1/inventory/suppliers")
                        .cookie(cashierCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        // Try update/delete also 403 (предварительно создадим через ADMIN роль)
        // Создаём ОТДЕЛЬНОЕ membership с ролью ADMIN, не мутируем cashier membership
        UserMembership umAdmin = new UserMembership();
        umAdmin.setUser(userRepo.findById(userId).orElseThrow());
        umAdmin.setMaster(m);
        umAdmin.setRole(RoleMembership.ADMIN);
        Long adminMem = membershipRepo.save(umAdmin).getId();
        Cookie adminCtx = switchContext(authCookie, adminMem);
        SupplierDto created = objectMapper.readValue(
                mockMvc.perform(post("/auth/v1/inventory/suppliers")
                                .cookie(adminCtx, ctx)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new SupplierDto(null, "S-1", null, null, null))))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(), SupplierDto.class);

        // Вернёмся к CASHIER и проверим запреты (используем исходный cashier membership id)
        Cookie forbiddenCtx = switchContext(authCookie, mem);
        mockMvc.perform(put("/auth/v1/inventory/suppliers/" + created.getId())
                        .cookie(forbiddenCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SupplierDto(created.getId(), "S-1x", null, null, null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/auth/v1/inventory/suppliers/" + created.getId())
                        .cookie(forbiddenCtx, ctx))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Дубликат имени в рамках master возвращает 400")
    void duplicateName_InSameMaster_400() throws Exception {
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M").status("ACTIVE").build());
        Long userId = userRepo.findByUsername(currentUsername).orElseThrow().getId();
        UserMembership um = new UserMembership();
        um.setUser(userRepo.findById(userId).orElseThrow());
        um.setMaster(m);
        um.setRole(RoleMembership.ADMIN);
        Long mem = membershipRepo.save(um).getId();
        Cookie adminCtx = switchContext(authCookie, mem);
        Cookie ctx = CtxTestCookies.createCtx(m.getId(), null, null, "change-me");

        SupplierDto dto = new SupplierDto(null, "DupName", null, null, null);
        mockMvc.perform(post("/auth/v1/inventory/suppliers")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Try create again with same name (case-insensitive)
        mockMvc.perform(post("/auth/v1/inventory/suppliers")
                        .cookie(adminCtx, ctx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SupplierDto(null, "dupname", null, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Без аутентификации список поставщиков недоступен (401)")
    void list_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/auth/v1/inventory/suppliers"))
                .andExpect(status().isUnauthorized());
    }
}
