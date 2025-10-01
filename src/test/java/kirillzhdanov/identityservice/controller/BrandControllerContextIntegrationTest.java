package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BrandControllerContextIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MasterAccountRepository masterRepo;

    private Cookie authCookie;

    private Cookie registerAndLogin() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("ctx-user");
        req.setEmail("ctx-user" + "@test.local");
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

    @BeforeEach
    void setup() throws Exception {
        authCookie = registerAndLogin();
    }

    @Test
    @DisplayName("Создание бренда и листинг работают в рамках X-Master-Id (контекст-лайт)")
    void createAndListScopedByMaster() throws Exception {
        // Arrange: два мастера
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());

        // Act: создаём бренд в M1
        BrandDto dto = BrandDto.builder().name("Brand-A").organizationName("OrgA").build();
        mockMvc.perform(post("/auth/v1/brands")
                        .cookie(authCookie)
                        .header("X-Master-Id", m1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Brand-A"));

        // Проверяем, что в M1 видим 1 бренд
        MvcResult listM1 = mockMvc.perform(get("/auth/v1/brands")
                        .cookie(authCookie)
                        .header("X-Master-Id", m1.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m1Brands = objectMapper.readValue(listM1.getResponse().getContentAsString(), List.class);
        assertThat(m1Brands).hasSize(1);

        // А в M2 список пуст
        MvcResult listM2 = mockMvc.perform(get("/auth/v1/brands")
                        .cookie(authCookie)
                        .header("X-Master-Id", m2.getId()))
                .andExpect(status().isOk())
                .andReturn();
        List<?> m2Brands = objectMapper.readValue(listM2.getResponse().getContentAsString(), List.class);
        assertThat(m2Brands).isEmpty();
    }

    @Test
    @DisplayName("Доступ к бренду другого мастера недоступен (404 в текущем контексте)")
    void getForeignMasterBrand_NotFound() throws Exception {
        // Arrange: создаём мастера и бренд под M1
        MasterAccount m1 = masterRepo.save(MasterAccount.builder().name("M1").status("ACTIVE").build());
        BrandDto dto = BrandDto.builder().name("Brand-X").organizationName("OrgX").build();
        MvcResult createdRes = mockMvc.perform(post("/auth/v1/brands")
                        .cookie(authCookie)
                        .header("X-Master-Id", m1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        BrandDto created = objectMapper.readValue(createdRes.getResponse().getContentAsString(), BrandDto.class);

        // Act: другой мастер
        MasterAccount m2 = masterRepo.save(MasterAccount.builder().name("M2").status("ACTIVE").build());
        mockMvc.perform(get("/auth/v1/brands/" + created.getId())
                        .cookie(authCookie)
                        .header("X-Master-Id", m2.getId()))
                .andExpect(status().is4xxClientError()); // 404 по нашей бизнес-логике
    }
}
