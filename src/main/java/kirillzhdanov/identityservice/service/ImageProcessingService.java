package kirillzhdanov.identityservice.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageProcessingService {

    public boolean isGif(byte[] data, String contentType) {
        if (contentType != null && contentType.equalsIgnoreCase("image/gif")) return true;
        if (data != null && data.length >= 6) {
            String header = new String(data, 0, 6);
            return header.startsWith("GIF87a") || header.startsWith("GIF89a");
        }
        return false;
    }

    public ProcessedResult processToPngSquare(byte[] data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) throw new IOException("Unsupported image format");

            int w = src.getWidth();
            int h = src.getHeight();
            int size = Math.min(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            BufferedImage cropped = src.getSubimage(x, y, size, size);

            Map<SizeKey, byte[]> out = new HashMap<>();
            out.put(SizeKey.S512, resizeToPng(cropped, 512));
            out.put(SizeKey.S256, resizeToPng(cropped, 256));
            out.put(SizeKey.S125, resizeToPng(cropped, 125));
            return new ProcessedResult(out, "image/png");
        }
    }

    public ProcessedResult processToPng16x9(byte[] data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) throw new IOException("Unsupported image format");

            int w = src.getWidth();
            int h = src.getHeight();
            // target aspect 16:9 => width/height = 16/9
            double targetAspect = 16.0 / 9.0;
            double srcAspect = (double) w / (double) h;

            int cropW, cropH;
            if (srcAspect > targetAspect) {
                // слишком широкое: обрезаем ширину
                cropH = h;
                cropW = (int) Math.round(h * targetAspect);
            } else {
                // слишком высокое или уже: обрезаем высоту
                cropW = w;
                cropH = (int) Math.round(w / targetAspect);
            }
            int x = (w - cropW) / 2;
            int y = (h - cropH) / 2;
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            if (cropW <= 0) cropW = Math.max(1, w);
            if (cropH <= 0) cropH = Math.max(1, h);
            BufferedImage cropped = src.getSubimage(x, y, cropW, cropH);

            // heights 256 and 512; compute corresponding widths for exact 16:9
            int h256 = 256;
            int w256 = (int) Math.round(h256 * targetAspect);
            int h512 = 512;
            int w512 = (int) Math.round(h512 * targetAspect);

            Map<SizeKey, byte[]> out = new HashMap<>();
            out.put(SizeKey.H512, resizeToJpeg(cropped, w512, h512, 0.9f));
            out.put(SizeKey.H256, resizeToJpeg(cropped, w256, h256, 0.9f));
            return new ProcessedResult(out, "image/jpeg");
        }
    }

    private byte[] resizeToPng(BufferedImage src, int target) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(src)
                    .size(target, target)
                    .outputFormat("png")
                    .toOutputStream(baos);
            return baos.toByteArray();
        }
    }

    private byte[] resizeToJpeg(BufferedImage src, int targetW, int targetH, float quality) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(src)
                    .forceSize(targetW, targetH)
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toOutputStream(baos);
            return baos.toByteArray();
        }
    }

    public enum SizeKey {S512, S256, S125, H512, H256}

        public record ProcessedResult(Map<SizeKey, byte[]> imagesBySize, String contentType) {

    }
}
