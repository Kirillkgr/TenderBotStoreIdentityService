package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.service.ImageProcessingService;
import kirillzhdanov.identityservice.service.MediaService;
import kirillzhdanov.identityservice.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaControllerTest {

    private MockMvc mockMvc;
    private MediaService mediaService;
    private ImageProcessingService imageProcessingService;
    private S3StorageService s3StorageService;

    @BeforeEach
    void setup() {
        mediaService = Mockito.mock(MediaService.class);
        imageProcessingService = Mockito.mock(ImageProcessingService.class);
        s3StorageService = Mockito.mock(S3StorageService.class);
        MediaController controller = new MediaController(imageProcessingService, s3StorageService, mediaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Upload product image - success")
    void uploadProduct_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "img.png", "image/png", new byte[]{1, 2, 3});
        MediaService.UploadResult result = new MediaService.UploadResult(
                "prod1", "img1", "png",
                Map.of(
                        "original", "product-images/prod1/img1/original.png",
                        "512", "product-images/prod1/img1/512.png",
                        "256", "product-images/prod1/img1/256.png",
                        "125", "product-images/prod1/img1/125.png"
                ),
                Map.of()
        );
        when(mediaService.uploadProduct(any(), anyString(), eq("prod1"), eq(true))).thenReturn(result);

        mockMvc.perform(multipart("/media/upload")
                        .file(file)
                        .param("productId", "prod1")
                        .param("publicForHomepage", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId", is("img1")))
                .andExpect(jsonPath("$.productId", is("prod1")))
                .andExpect(jsonPath("$.keys.original", containsString("original")))
                .andExpect(jsonPath("$.keys['512']", org.hamcrest.Matchers.endsWith("512.png")));
    }

    @Test
    @DisplayName("Upload product image - empty file -> 400")
    void uploadProduct_emptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/png", new byte[]{});
        mockMvc.perform(multipart("/media/upload")
                        .file(file)
                        .param("productId", "p"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Empty file")));
    }

    @Test
    @DisplayName("Overwrite product - success")
    void overwriteProduct_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "img.png", "image/png", new byte[]{1});
        MediaService.UploadResult result = new MediaService.UploadResult(
                "prod1", "img1", "png",
                Map.of("512", "product-images/prod1/img1/512.png"), Map.of());
        when(mediaService.overwriteProduct(any(), anyString(), eq("prod1"), eq("img1"), eq(false))).thenReturn(result);

        mockMvc.perform(multipart("/media/product/overwrite")
                        .file(file)
                        .param("productId", "prod1")
                        .param("imageId", "img1")
                        .param("publicForHomepage", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId", is("img1")))
                .andExpect(jsonPath("$.productId", is("prod1")));
    }

    @Test
    @DisplayName("Delete product derived - success")
    void deleteProductDerived_success() throws Exception {
        mockMvc.perform(delete("/media/product/derived")
                        .param("productId", "prod1")
                        .param("imageId", "img1"))
                .andExpect(status().isOk());
        Mockito.verify(mediaService).deleteDerivedProduct("prod1", "img1");
    }

    @Test
    @DisplayName("Regenerate product from original - success")
    void regenerateProduct_success() throws Exception {
        MediaService.UploadResult result = new MediaService.UploadResult(
                "prod1", "img1", "png",
                Map.of("512", "product-images/prod1/img1/512.png"), Map.of());
        when(mediaService.regenerateProductFromOriginal("prod1", "img1", true)).thenReturn(result);

        mockMvc.perform(post("/media/product/regenerate")
                        .param("productId", "prod1")
                        .param("imageId", "img1")
                        .param("publicForHomepage", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId", is("img1")))
                .andExpect(jsonPath("$.productId", is("prod1")));
    }

    @Test
    @DisplayName("Hard delete product - success")
    void hardDeleteProduct_success() throws Exception {
        mockMvc.perform(delete("/media/product/hard")
                        .param("productId", "prod1")
                        .param("imageId", "img1"))
                .andExpect(status().isOk());
        Mockito.verify(mediaService).hardDeleteProduct("prod1", "img1");
    }

    // ================== BATCH tests ==================

    @Test
    @DisplayName("Batch delete product derived - success")
    void batchDeleteProductDerived_success() throws Exception {
        String body = "[ {\"productId\":\"p1\",\"imageId\":\"i1\"}, {\"productId\":\"p2\",\"imageId\":\"i2\"} ]";
        mockMvc.perform(post("/media/product/derived-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedDerived", is(2)));
        Mockito.verify(mediaService).deleteDerivedProduct("p1", "i1");
        Mockito.verify(mediaService).deleteDerivedProduct("p2", "i2");
    }

    @Test
    @DisplayName("Batch regenerate product - success")
    void batchRegenerateProduct_success() throws Exception {
        var r1 = new MediaService.UploadResult("p1", "i1", "png", Map.of(), Map.of());
        var r2 = new MediaService.UploadResult("p2", "i2", "png", Map.of(), Map.of());
        Mockito.when(mediaService.regenerateProductFromOriginal("p1", "i1", true)).thenReturn(r1);
        Mockito.when(mediaService.regenerateProductFromOriginal("p2", "i2", true)).thenReturn(r2);

        String body = "[ {\"productId\":\"p1\",\"imageId\":\"i1\"}, {\"productId\":\"p2\",\"imageId\":\"i2\"} ]";
        mockMvc.perform(post("/media/product/regenerate-batch")
                        .queryParam("publicForHomepage", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].productId", is("p1")))
                .andExpect(jsonPath("$.results[1].productId", is("p2")));
    }

    @Test
    @DisplayName("Batch hard delete product - success")
    void batchHardDeleteProduct_success() throws Exception {
        String body = "[ {\"productId\":\"p1\",\"imageId\":\"i1\"}, {\"productId\":\"p2\",\"imageId\":\"i2\"} ]";
        mockMvc.perform(post("/media/product/hard-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hardDeleted", is(2)));
        Mockito.verify(mediaService).hardDeleteProduct("p1", "i1");
        Mockito.verify(mediaService).hardDeleteProduct("p2", "i2");
    }

    @Test
    @DisplayName("Batch delete tag derived - success")
    void batchDeleteTagDerived_success() throws Exception {
        String body = "[ {\"tagGroupId\":\"t1\",\"imageId\":\"i1\"}, {\"tagGroupId\":\"t2\",\"imageId\":\"i2\"} ]";
        mockMvc.perform(post("/media/tag/derived-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedDerived", is(2)));
        Mockito.verify(mediaService).deleteDerivedTagGroup("t1", "i1");
        Mockito.verify(mediaService).deleteDerivedTagGroup("t2", "i2");
    }

    @Test
    @DisplayName("Batch regenerate tag - success")
    void batchRegenerateTag_success() throws Exception {
        var r1 = new MediaService.UploadResult("t1", "i1", "png", Map.of(), Map.of());
        var r2 = new MediaService.UploadResult("t2", "i2", "png", Map.of(), Map.of());
        Mockito.when(mediaService.regenerateTagGroupFromOriginal("t1", "i1", false)).thenReturn(r1);
        Mockito.when(mediaService.regenerateTagGroupFromOriginal("t2", "i2", false)).thenReturn(r2);

        String body = "[ {\"tagGroupId\":\"t1\",\"imageId\":\"i1\"}, {\"tagGroupId\":\"t2\",\"imageId\":\"i2\"} ]";
        mockMvc.perform(post("/media/tag/regenerate-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].tagGroupId", is("t1")))
                .andExpect(jsonPath("$.results[1].tagGroupId", is("t2")));
    }

    @Test
    @DisplayName("Batch hard delete tag - success")
    void batchHardDeleteTag_success() throws Exception {
        String body = "[ {\"tagGroupId\":\"t1\",\"imageId\":\"i1\"}, {\"tagGroupId\":\"t2\",\"imageId\":\"i2\"} ]";
        mockMvc.perform(post("/media/tag/hard-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hardDeleted", is(2)));
        Mockito.verify(mediaService).hardDeleteTagGroup("t1", "i1");
        Mockito.verify(mediaService).hardDeleteTagGroup("t2", "i2");
    }
}
