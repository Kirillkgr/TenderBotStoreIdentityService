package kirillzhdanov.identityservice.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ImageProcessingServiceTest {

    private final ImageProcessingService service = new ImageProcessingService();

    @Test
    void processToPngSquare_producesThreeSquarePngs() throws IOException {
        // Create 300x200 image (wider than tall) to verify center-crop to square
        BufferedImage src = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = src.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 300, 200);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(src, "png", baos);
        byte[] bytes = baos.toByteArray();

        ImageProcessingService.ProcessedResult result = service.processToPngSquare(bytes);
        assertEquals("image/png", result.getContentType());
        Map<ImageProcessingService.SizeKey, byte[]> map = result.getImagesBySize();
        assertTrue(map.containsKey(ImageProcessingService.SizeKey.S512));
        assertTrue(map.containsKey(ImageProcessingService.SizeKey.S256));
        assertTrue(map.containsKey(ImageProcessingService.SizeKey.S125));

        // Verify dimensions are exact and square
        for (var entry : map.entrySet()) {
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(entry.getValue()));
            int expected = switch (entry.getKey()) {
                case S512 -> 512;
                case S256 -> 256;
                case S125 -> 125;
            };
            assertEquals(expected, img.getWidth());
            assertEquals(expected, img.getHeight());
        }
    }

    @Test
    void isGif_detectsGifByHeaderOrContentType() {
        byte[] header = "GIF89aXXXX".getBytes();
        assertTrue(service.isGif(header, null));
        assertTrue(service.isGif(new byte[0], "image/gif"));
        assertFalse(service.isGif("notgif".getBytes(), "image/png"));
    }
}
