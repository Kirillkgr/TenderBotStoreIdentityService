package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TenantEnforcementIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MasterAccountRepository masterRepo;
    @Autowired
    private UserMembershipRepository membershipRepo;
    @Autowired
    private UserRepository userRepo;

    private Cookie registerAndLogin(String username) throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername(username);
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

    @Test
    @DisplayName("Public endpoints pass without context")
    void publicEndpoints_NoContext_Ok() throws Exception {
        mockMvc.perform(get("/status")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoint without auth -> 401")
    void protected_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/auth/v1/brands"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected brands endpoint with auth but no context -> 200 (master auto-derived)")
    void protected_Auth_NoContext_AllowsBrands() throws Exception {
        Cookie authCookie = registerAndLogin("enf-user");
        mockMvc.perform(get("/auth/v1/brands").cookie(authCookie))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoint with auth and context -> 200")
    void protected_Auth_WithContext_Ok() throws Exception {
        Cookie authCookie = registerAndLogin("enf-user2");
        // prepare membership
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        Long userId = userRepo.findByUsername("enf-user2").orElseThrow().getId();
        UserMembership mem = new UserMembership();
        mem.setUser(userRepo.findById(userId).orElseThrow());
        mem.setMaster(m);
        Long membershipId = membershipRepo.save(mem).getId();

        ContextSwitchRequest req = new ContextSwitchRequest();
        req.setMembershipId(membershipId);
        MvcResult res = mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        String accessToken = objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
        Cookie ctxCookie = new Cookie("accessToken", accessToken);

        mockMvc.perform(get("/auth/v1/brands").cookie(ctxCookie))
                .andExpect(status().isOk());
    }
}
