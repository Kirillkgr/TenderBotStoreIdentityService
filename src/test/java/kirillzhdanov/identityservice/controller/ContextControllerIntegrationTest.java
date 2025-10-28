package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.ContextSwitchRequest;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ContextControllerIntegrationTest extends IntegrationTestBase {

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
    @Autowired
    private BrandRepository brandRepo;
    @Autowired
    private PickupPointRepository pickupRepo;

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

    private Long userIdByUsername(String username) {
        return userRepo.findByUsername(username).orElseThrow().getId();
    }

    private Long createMembership(Long userId, Long masterId, Long brandId, Long locationId) {
        UserMembership m = new UserMembership();
        m.setUser(userRepo.findById(userId).orElseThrow());
        m.setMaster(masterRepo.findById(masterId).orElseThrow());
        if (brandId != null) m.setBrand(brandRepo.findById(brandId).orElseThrow());
        if (locationId != null) m.setPickupPoint(pickupRepo.findById(locationId).orElseThrow());
        return membershipRepo.save(m).getId();
    }

    @BeforeEach
    void setup() {
    }

    @Test
    @DisplayName("context/switch success returns accessToken with context claims")
    void contextSwitch_Success() throws Exception {
        Cookie authCookie = registerAndLogin("ctx-user");
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        Long userId = userIdByUsername("ctx-user");
        Long membershipId = createMembership(userId, m.getId(), null, null);

        ContextSwitchRequest req = new ContextSwitchRequest();
        req.setMembershipId(membershipId);

        MvcResult res = mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        assertThat(json).contains("accessToken");
    }

    @Test
    @DisplayName("context/switch without membershipId -> 400")
    void contextSwitch_NoMembershipId_BadRequest() throws Exception {
        Cookie authCookie = registerAndLogin("ctx-user2");
        ContextSwitchRequest req = new ContextSwitchRequest();
        mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("context/switch membership not belongs to user -> 400")
    void contextSwitch_MembershipForeign_BadRequest() throws Exception {
        Cookie authCookie = registerAndLogin("u1");
        Cookie otherAuth = registerAndLogin("u2");
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        Long user1 = userIdByUsername("u1");
        Long user2 = userIdByUsername("u2");
        Long membershipOfUser2 = createMembership(user2, m.getId(), null, null);

        ContextSwitchRequest req = new ContextSwitchRequest();
        req.setMembershipId(membershipOfUser2);
        mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(authCookie) // trying with user1
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("context/switch override brand/location mismatch -> 400")
    void contextSwitch_OverrideMismatch_BadRequest() throws Exception {
        Cookie authCookie = registerAndLogin("u3");
        MasterAccount m = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        // membership with specific brand/location
        Brand b1 = brandRepo.save(Brand.builder().name("B1").organizationName("Org1").master(m).build());
        PickupPoint p1 = pickupRepo.save(PickupPoint.builder().brand(b1).name("P1").address("A").latitude(new BigDecimal("1.0")).longitude(new BigDecimal("2.0")).active(true).build());
        Long userId = userIdByUsername("u3");
        Long membershipId = createMembership(userId, m.getId(), b1.getId(), p1.getId());

        // create another brand/location to pass as override mismatch
        Brand b2 = brandRepo.save(Brand.builder().name("B2").organizationName("Org2").master(m).build());
        PickupPoint p2 = pickupRepo.save(PickupPoint.builder().brand(b2).name("P2").address("A2").latitude(new BigDecimal("3.0")).longitude(new BigDecimal("4.0")).active(true).build());

        ContextSwitchRequest req = new ContextSwitchRequest();
        req.setMembershipId(membershipId);
        req.setBrandId(b2.getId()); // mismatch
        req.setLocationId(p2.getId()); // mismatch

        mockMvc.perform(post("/auth/v1/context/switch")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
