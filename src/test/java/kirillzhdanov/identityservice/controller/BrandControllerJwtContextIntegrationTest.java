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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BrandControllerJwtContextIntegrationTest extends IntegrationTestBase {

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

    private Cookie registerAndLogin() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("jwtctx-user");
        req.setEmail("jwtctx-user" + "@test.local");
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
    @DisplayName("JWT-контекст: изоляция брендов по master")
    void brandsIsolationByMaster_WithJwtContext() throws Exception {
        Cookie login = registerAndLogin();
        Long userId = userRepo.findByUsername("jwtctx-user").orElseThrow().getId();

        // Два мастера и членства
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());
        UserMembership um1 = new UserMembership();
        um1.setUser(userRepo.findById(userId).orElseThrow());
        um1.setMaster(m1);
        UserMembership um2 = new UserMembership();
        um2.setUser(userRepo.findById(userId).orElseThrow());
        um2.setMaster(m2);
        Long mem1 = membershipRepo.save(um1).getId();
        Long mem2 = membershipRepo.save(um2).getId();

        // Переключаемся в контекст M1
        Cookie ctxM1 = switchContext(login, mem1);

        // Создаём бренд в M1
        BrandDto dto = BrandDto.builder().name("Brand-JWT-A").organizationName("OrgA").build();
        mockMvc.perform(post("/auth/v1/brands")
                        .cookie(ctxM1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Brand-JWT-A"));

        // Листинг в M1 содержит бренд
        MvcResult listM1 = mockMvc.perform(get("/auth/v1/brands").cookie(ctxM1))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m1Brands = objectMapper.readValue(listM1.getResponse().getContentAsString(), List.class);
        assertThat(m1Brands).hasSize(1);

        // Переключаемся в контекст M2 и проверяем изоляцию
        Cookie ctxM2 = switchContext(login, mem2);
        MvcResult listM2 = mockMvc.perform(get("/auth/v1/brands").cookie(ctxM2))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m2Brands = objectMapper.readValue(listM2.getResponse().getContentAsString(), List.class);
        assertThat(m2Brands).isEmpty();
    }
}
