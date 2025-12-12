package kirillzhdanov.identityservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MediaServiceTest {

    private S3StorageService s3;
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        ImageProcessingService imageProcessingService = new ImageProcessingService();
        s3 = mock(S3StorageService.class);
        mediaService = new MediaService(imageProcessingService, s3);

        // For public URL composition, return base + key
        when(s3.buildPublicUrl(anyString())).thenAnswer(inv -> Optional.of("https://storage.yandexcloud.net/tbspro.ru/" + inv.getArgument(0)));
    }

    @Test
    void uploadProduct_png_generatesSizesAndOriginal() throws IOException {
        byte[] png = TestImages.redPng(300, 200);
        var res = mediaService.uploadProduct(png, "image/png", "prod123", true);
        assertEquals("png", res.format());
        assertEquals("prod123", res.productId());
        assertNotNull(res.imageId());
        Map<String, String> keys = res.keys();
        assertTrue(keys.get("original").contains("product-images/prod123/"));
        assertTrue(keys.get("512").endsWith("512.png"));
        assertTrue(keys.get("256").endsWith("256.png"));
        assertTrue(keys.get("125").endsWith("125.png"));

        // Verify uploads were called for all 4 artifacts
        verify(s3, atLeast(4)).upload(anyString(), any(), anyString(), eq(true));
    }

    @Test
    void uploadProduct_gif_storesOriginalAndCopies() throws IOException {
        byte[] gif = TestImages.gifHeaderBytes();
        var res = mediaService.uploadProduct(gif, "image/gif", "prod777", false);
        assertEquals("gif", res.format());
        Map<String, String> keys = res.keys();
        assertTrue(keys.get("original").endsWith("original.gif"));
        assertTrue(keys.get("512").endsWith("512.gif"));
        assertTrue(keys.get("256").endsWith("256.gif"));
        assertTrue(keys.get("125").endsWith("125.gif"));
        verify(s3, atLeast(4)).upload(anyString(), any(), anyString(), eq(false));
    }

    @Test
    void overwriteProduct_addsVersionParamToUrls() throws IOException {
        byte[] png = TestImages.redPng(100, 100);
        var res = mediaService.overwriteProduct(png, "image/png", "prod1", "img1", true);
        // URLs are clean (no version param)
        assertTrue(res.urls().values().stream().noneMatch(u -> u.contains("?v=")));
    }

    @Test
    void regenerateFromOriginal_loadsAndRebuilds() throws IOException {
        // Arrange: simulate existing original
        String base = "product-images/prodX/imgX/";
        when(s3.listKeysByPrefix(base + "original")).thenReturn(java.util.List.of(base + "original.png"));
        when(s3.getObjectBytes(base + "original.png")).thenReturn(TestImages.redPng(200, 200));

        // Act
        var res = mediaService.regenerateProductFromOriginal("prodX", "imgX", true);

        // Assert: urls are clean (no version param)
        assertTrue(res.urls().values().stream().noneMatch(u -> u.contains("?v=")));
        verify(s3, atLeast(3)).upload(startsWith(base), any(), anyString(), eq(true));
    }

    // Helper inner class to generate simple images/bytes
    static class TestImages {
        static byte[] redPng(int w, int h) throws IOException {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.RED);
            g.fillRect(0, 0, w, h);
            g.dispose();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        }

        static byte[] gifHeaderBytes() {
            return "GIF89a".getBytes();
        }
    }
}
