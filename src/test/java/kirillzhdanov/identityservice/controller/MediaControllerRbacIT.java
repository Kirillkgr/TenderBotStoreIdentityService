package kirillzhdanov.identityservice.controller;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.service.MediaService;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("RBAC: MediaController security (auth + tenant context)")
@Tag("rbac-media")
class MediaControllerRbacIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    MembershipFixtures fx;

    @Autowired
    MediaService mediaService;

    @Test
    @DisplayName("POST /media/upload: 401 без аутентификации")
    void upload_unauthorized_without_auth() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "img.png", "image/png", new byte[]{1, 2});
        mvc.perform(multipart("/media/upload")
                        .file(file)
                        .param("productId", "p1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /media/upload: 403 аутентифицирован без X-Master-Id")
    void upload_forbidden_without_master_context() throws Exception {
        String username = "media-ctx-" + System.nanoTime();
        Cookie login = fx.registerAndLogin(username);
        MockMultipartFile file = new MockMultipartFile("file", "img.png", "image/png", new byte[]{1, 2});
        mvc.perform(multipart("/media/upload")
                        .file(file)
                        .param("productId", "p1")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue()))
                .andExpect(status().isForbidden()); // ContextEnforcementFilter требует X-Master-Id
    }

    @Test
    @DisplayName("POST /media/upload: 200 для ADMIN при наличии X-Master-Id")
    void upload_ok_with_admin_and_context() throws Exception {
        String username = "media-admin-" + System.nanoTime();
        Cookie login = fx.registerAndLogin(username);
        var ctx = fx.prepareRoleMembership(login, username, RoleMembership.ADMIN);

        MockMultipartFile file = new MockMultipartFile("file", "img.png", "image/png", new byte[]{1, 2, 3});
        MediaService.UploadResult result = new MediaService.UploadResult(
                "prod1", "img1", "png",
                Map.of("original", "product-images/prod1/img1/original.png"),
                Map.of()
        );
        when(mediaService.uploadProduct(any(byte[].class), anyString(), eq("p1"), eq(false))).thenReturn(result);

        mvc.perform(multipart("/media/upload")
                        .file(file)
                        .param("productId", "p1")
                        .param("publicForHomepage", "false")
                        .cookie(ctx.cookie())
                        .header("Authorization", "Bearer " + ctx.cookie().getValue())
                        .header("X-Master-Id", ctx.masterId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId").value("img1"))
                .andExpect(jsonPath("$.productId").value("prod1"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        MediaService mediaService() {
            return org.mockito.Mockito.mock(MediaService.class);
        }
    }
}
