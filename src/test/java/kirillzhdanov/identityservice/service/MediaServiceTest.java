package kirillzhdanov.identityservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Test
    void uploadProductImage_original_h256_h512_areUploaded_withCorrectDimensions() throws Exception {
        // Capture uploaded bytes by key
        Map<String, byte[]> uploaded = new HashMap<>();
        doAnswer(inv -> {
            String key = inv.getArgument(0);
            byte[] data = inv.getArgument(1);
            uploaded.put(key, data);
            return null;
        }).when(s3).upload(anyString(), any(byte[].class), anyString(), eq(false));

        // Load provided test image
        Path imgPath = Path.of("src", "test", "resources", "test_img.png");
        byte[] bytes = Files.readAllBytes(imgPath);

        // Act
        Map<String, String> res = mediaService.uploadProductImage("prod999", bytes, "image/png");

        String base = "product-images/prod999/";
        assertEquals(base + "original.png", res.get("ORIGINAL"));
        assertEquals(base + "h256.jpg", res.get("H256"));
        assertEquals(base + "h512.jpg", res.get("H512"));

        // Verify uploads
        verify(s3).upload(eq(base + "original.png"), any(), eq("image/png"), eq(false));
        // Note: MediaService chooses variant content type from processed result (image/jpeg)
        verify(s3).upload(eq(base + "h256.jpg"), any(), anyString(), eq(false));
        verify(s3).upload(eq(base + "h512.jpg"), any(), anyString(), eq(false));

        // Validate dimensions of derived images
        byte[] h256Bytes = uploaded.get(base + "h256.jpg");
        byte[] h512Bytes = uploaded.get(base + "h512.jpg");
        assertNotNull(h256Bytes);
        assertNotNull(h512Bytes);
        try (ByteArrayInputStream in256 = new ByteArrayInputStream(h256Bytes);
             ByteArrayInputStream in512 = new ByteArrayInputStream(h512Bytes)) {
            BufferedImage i256 = ImageIO.read(in256);
            BufferedImage i512 = ImageIO.read(in512);
            assertNotNull(i256);
            assertNotNull(i512);
            assertEquals(256, i256.getHeight());
            assertEquals(512, i512.getHeight());
            assertEquals((int) Math.round(256 * (16.0 / 9.0)), i256.getWidth());
            assertEquals((int) Math.round(512 * (16.0 / 9.0)), i512.getWidth());
        }
    }

    @Test
    void uploadProductImage_secondUpload_overwritesSameKeys() throws Exception {
        doNothing().when(s3).upload(anyString(), any(byte[].class), anyString(), eq(false));

        byte[] img1 = TestImages.redPng(800, 600);
        byte[] img2 = TestImages.redPng(640, 360);

        String base = "product-images/prodABC/";

        // First upload
        mediaService.uploadProductImage("prodABC", img1, "image/png");
        // Second upload (replace)
        mediaService.uploadProductImage("prodABC", img2, "image/png");

        // Same keys should be uploaded at least twice across calls
        verify(s3, atLeast(2)).upload(eq(base + "original.png"), any(), anyString(), eq(false));
        verify(s3, atLeast(2)).upload(eq(base + "h256.jpg"), any(), anyString(), eq(false));
        verify(s3, atLeast(2)).upload(eq(base + "h512.jpg"), any(), anyString(), eq(false));
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
