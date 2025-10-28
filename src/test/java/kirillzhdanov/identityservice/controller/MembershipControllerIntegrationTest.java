package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.MembershipDto;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MembershipControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
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

    @Test
    @DisplayName("GET /auth/v1/memberships returns user memberships (auth required)")
    void memberships_Success() throws Exception {
        Cookie cookie = registerAndLogin("mem-user");
        Long userId = userRepository.findByUsername("mem-user").orElseThrow().getId();
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        UserMembership um = new UserMembership();
        um.setUser(userRepository.findById(userId).orElseThrow());
        um.setMaster(m);
        membershipRepo.save(um);

        MvcResult res = mockMvc.perform(get("/auth/v1/memberships").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        List<MembershipDto> list = objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<>() {
        });
        // Регистрация создаёт дефолтное OWNER-членство для master с именем пользователя.
        // Проверяем, что присутствует членство по созданному нами мастеру M1.
        assertThat(list.stream().anyMatch(it -> it.getMasterId().equals(m.getId()))).isTrue();
    }

    @Test
    @DisplayName("GET /auth/v1/memberships unauthorized without auth")
    void memberships_Unauthorized() throws Exception {
        mockMvc.perform(get("/auth/v1/memberships"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/v1/memberships returns empty list when user has none")
    void memberships_Empty() throws Exception {
        Cookie cookie = registerAndLogin("mem-empty");
        MvcResult res = mockMvc.perform(get("/auth/v1/memberships").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        List<MembershipDto> list = objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<>() {
        });
        // После регистрации есть дефолтное OWNER-членство для master=имя пользователя
        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().getMasterName()).isEqualTo("mem-empty");
    }

    @Test
    @DisplayName("GET /auth/v1/memberships returns multiple memberships with brand/location fields")
    void memberships_MultipleWithBrandLocation() throws Exception {
        Cookie cookie = registerAndLogin("mem-many");
        Long userId = userRepository.findByUsername("mem-many").orElseThrow().getId();
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());

        // Create two memberships for the same user
        UserMembership um1 = new UserMembership();
        um1.setUser(userRepository.findById(userId).orElseThrow());
        um1.setMaster(m1);
        membershipRepo.save(um1);

        UserMembership um2 = new UserMembership();
        um2.setUser(userRepository.findById(userId).orElseThrow());
        um2.setMaster(m2);
        membershipRepo.save(um2);

        MvcResult res = mockMvc.perform(get("/auth/v1/memberships").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        List<MembershipDto> list = objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<>() {
        });
        // Помимо дефолтного OWNER-членства, ожидаем наличие M1 и M2
        assertThat(list.stream().map(MembershipDto::getMasterName)).contains("M1", "M2");
    }
}
