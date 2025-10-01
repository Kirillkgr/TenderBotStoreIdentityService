package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.dto.ContextSwitchRequest;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ErrorCodesStandardizationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private MasterAccountRepository masterRepo;
    @Autowired
    private UserMembershipRepository membershipRepo;

    private Cookie registerAndLogin(String username) throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername(username);
        req.setEmail(username + "@test.local");
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

    private Cookie switchContext(Cookie authCookie, Long membershipId) throws Exception {
        ContextSwitchRequest req = new ContextSwitchRequest();
        req.setMembershipId(membershipId);
        MvcResult sw = mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(sw.getResponse().getContentAsString()).get("accessToken").asText();
        return new Cookie("accessToken", accessToken);
    }

    @Test
    @DisplayName("401: защищённый эндпоинт без токена")
    void shouldReturn401WhenNoToken() throws Exception {
        // любой защищённый GET — список брендов
        mockMvc.perform(get("/auth/v1/brands"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403: недостаточно прав на админ-операцию (RBAC)")
    void shouldReturn403WhenNoAdminRights() throws Exception {
        Cookie login = registerAndLogin("codes-user1");
        Long userId = userRepo.findByUsername("codes-user1").orElseThrow().getId();
        MasterAccount master = masterRepo.save(MasterAccount.builder().name("M403").status("ACTIVE").build());

        // membership с не-админской ролью (CLIENT)
        UserMembership um = new UserMembership();
        um.setUser(userRepo.findById(userId).orElseThrow());
        um.setMaster(master);
        um.setRole(RoleMembership.CLIENT);
        Long memId = membershipRepo.save(um).getId();
        Cookie clientCtx = switchContext(login, memId);

        // Пытаемся создать бренд -> 403
        BrandDto dto = BrandDto.builder().name("RBAC-Brand").organizationName("RBAC-Org").build();
        mockMvc.perform(post("/auth/v1/brands")
                        .cookie(clientCtx)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404: доступ к чужой сущности маскируется как не найдено")
    void shouldReturn404ForForeignEntity() throws Exception {
        Cookie login = registerAndLogin("codes-user2");
        Long userId = userRepo.findByUsername("codes-user2").orElseThrow().getId();

        // Два мастера и два membership (в каждом по ADMIN)
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M404-1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M404-2").status("ACTIVE").build());
        UserMembership um1 = new UserMembership();
        um1.setUser(userRepo.findById(userId).orElseThrow());
        um1.setMaster(m1);
        um1.setRole(RoleMembership.ADMIN);
        Long mem1 = membershipRepo.save(um1).getId();

        UserMembership um2 = new UserMembership();
        um2.setUser(userRepo.findById(userId).orElseThrow());
        um2.setMaster(m2);
        um2.setRole(RoleMembership.ADMIN);
        Long mem2 = membershipRepo.save(um2).getId();

        // В контексте M1 создаём бренд
        Cookie ctxM1 = switchContext(login, mem1);
        BrandDto dto = BrandDto.builder().name("Foreign-Brand").organizationName("Foreign-Org").build();
        MvcResult created = mockMvc.perform(post("/auth/v1/brands")
                        .cookie(ctxM1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        long brandId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Переключаемся в контекст M2 и пытаемся получить бренд M1 -> 404
        Cookie ctxM2 = switchContext(login, mem2);
        mockMvc.perform(get("/auth/v1/brands/" + brandId).cookie(ctxM2))
                .andExpect(status().is4xxClientError()) // допускаем 404 по бизнес-логике маскировки
                .andExpect(status().isNotFound());
    }
}
