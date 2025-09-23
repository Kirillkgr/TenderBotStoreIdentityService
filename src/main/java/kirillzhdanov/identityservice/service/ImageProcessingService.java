package kirillzhdanov.identityservice.service;

import lombok.Data;
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

    private byte[] resizeToPng(BufferedImage src, int target) throws IOException {
        BufferedImage outImg = Thumbnails.of(src)
                .size(target, target)
                .outputFormat("png")
                .asBufferedImage();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(outImg, "png", baos);
            return baos.toByteArray();
        }
    }

    public enum SizeKey {S512, S256, S125}

    @Data
    public static class ProcessedResult {
        private final Map<SizeKey, byte[]> imagesBySize;
        private final String contentType;

        public ProcessedResult(Map<SizeKey, byte[]> imagesBySize, String contentType) {
            this.imagesBySize = imagesBySize;
            this.contentType = contentType;
        }

    }
}
