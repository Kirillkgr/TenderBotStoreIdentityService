package kirillzhdanov.identityservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final ImageProcessingService imageService;
    private final S3StorageService s3;

    public UploadResult uploadProduct(byte[] bytes, String contentType, String productId, boolean publicForHomepage) throws IOException {
        String imageId = UUID.randomUUID().toString();
        String basePath = baseProductPath(productId, imageId);
        return uploadCommon(bytes, contentType, basePath, publicForHomepage, imageId, productId);
    }

    public UploadResult overwriteProduct(byte[] bytes, String contentType, String productId, String imageId, boolean publicForHomepage) throws IOException {
        String basePath = baseProductPath(productId, imageId);
        return uploadCommon(bytes, contentType, basePath, publicForHomepage, imageId, productId, false);
    }

    public void deleteDerivedProduct(String productId, String imageId) {
        String basePath = baseProductPath(productId, imageId);
        // delete sizes, keep original.*
        s3.deleteByPrefix(basePath + "512");
        s3.deleteByPrefix(basePath + "256");
        s3.deleteByPrefix(basePath + "125");
    }

    public UploadResult regenerateProductFromOriginal(String productId, String imageId, boolean publicForHomepage) throws IOException {
        String basePath = baseProductPath(productId, imageId);
        Optional<String> originalKey = findOriginalKey(basePath);
        if (originalKey.isEmpty()) throw new IllegalStateException("Original not found for imageId=" + imageId);
        byte[] original = s3.getObjectBytes(originalKey.get());
        String contentType = contentTypeFromExt(originalKey.get());
        // remove existing derived first
        deleteDerivedProduct(productId, imageId);
        return uploadCommon(original, contentType, basePath, publicForHomepage, imageId, productId, true);
    }

    public void hardDeleteProduct(String productId, String imageId) {
        String basePath = baseProductPath(productId, imageId);
        s3.deleteByPrefix(basePath); // delete everything including original
    }

    // Tag Group equivalents
    public UploadResult uploadTagGroup(byte[] bytes, String contentType, String tagGroupId, boolean publicForHomepage) throws IOException {
        String imageId = UUID.randomUUID().toString();
        String basePath = baseTagPath(tagGroupId, imageId);
        return uploadCommon(bytes, contentType, basePath, publicForHomepage, imageId, tagGroupId);
    }

    public UploadResult overwriteTagGroup(byte[] bytes, String contentType, String tagGroupId, String imageId, boolean publicForHomepage) throws IOException {
        String basePath = baseTagPath(tagGroupId, imageId);
        return uploadCommon(bytes, contentType, basePath, publicForHomepage, imageId, tagGroupId, false);
    }

    public void deleteDerivedTagGroup(String tagGroupId, String imageId) {
        String basePath = baseTagPath(tagGroupId, imageId);
        s3.deleteByPrefix(basePath + "512");
        s3.deleteByPrefix(basePath + "256");
        s3.deleteByPrefix(basePath + "125");
    }

    public UploadResult regenerateTagGroupFromOriginal(String tagGroupId, String imageId, boolean publicForHomepage) throws IOException {
        String basePath = baseTagPath(tagGroupId, imageId);
        Optional<String> originalKey = findOriginalKey(basePath);
        if (originalKey.isEmpty()) throw new IllegalStateException("Original not found for imageId=" + imageId);
        byte[] original = s3.getObjectBytes(originalKey.get());
        String contentType = contentTypeFromExt(originalKey.get());
        deleteDerivedTagGroup(tagGroupId, imageId);
        return uploadCommon(original, contentType, basePath, publicForHomepage, imageId, tagGroupId, true);
    }

    public void hardDeleteTagGroup(String tagGroupId, String imageId) {
        String basePath = baseTagPath(tagGroupId, imageId);
        s3.deleteByPrefix(basePath);
    }

    private UploadResult uploadCommon(byte[] bytes, String contentType, String basePath, boolean publicForHomepage,
                                      String imageId, String ownerId) throws IOException {
        return uploadCommon(bytes, contentType, basePath, publicForHomepage, imageId, ownerId, false);
    }

    private UploadResult uploadCommon(byte[] bytes, String contentType, String basePath, boolean publicForHomepage,
                                      String imageId, String ownerId, boolean keepOriginal) throws IOException {
        Map<String, String> resultUrls = new HashMap<>();
        Map<String, String> resultKeys = new HashMap<>();

        boolean isGif = imageService.isGif(bytes, contentType);
        if (isGif) {
            String originalKey = basePath + "original.gif";
            if (!keepOriginal) {
                s3.upload(originalKey, bytes, "image/gif", publicForHomepage);
            }
            resultKeys.put("original", originalKey);
            s3.buildPublicUrl(originalKey).ifPresent(u -> resultUrls.put("original", u));

            for (String size : new String[]{"512", "256", "125"}) {
                String key = basePath + size + ".gif";
                s3.upload(key, bytes, "image/gif", publicForHomepage);
                resultKeys.put(size, key);
                s3.buildPublicUrl(key).ifPresent(u -> resultUrls.put(size, u));
            }
            return new UploadResult(ownerId, imageId, "gif", resultKeys, resultUrls);
        }

        // Non-GIF: original as-is
        String originalExt = extFromContentType(contentType);
        String originalKey = basePath + "original" + (originalExt.isEmpty() ? "" : "." + originalExt);
        if (!keepOriginal) {
            s3.upload(originalKey, bytes, contentType != null ? contentType : "application/octet-stream", publicForHomepage);
        }
        resultKeys.put("original", originalKey);
        s3.buildPublicUrl(originalKey).ifPresent(u -> resultUrls.put("original", u));

        ImageProcessingService.ProcessedResult processed = imageService.processToPngSquare(bytes);
        Map<ImageProcessingService.SizeKey, byte[]> map = processed.getImagesBySize();
        String[] ordered = {"512", "256", "125"};
        ImageProcessingService.SizeKey[] keys = {ImageProcessingService.SizeKey.S512, ImageProcessingService.SizeKey.S256, ImageProcessingService.SizeKey.S125};
        for (int i = 0; i < ordered.length; i++) {
            final String sizeName = ordered[i];
            String key = basePath + sizeName + ".png";
            s3.upload(key, map.get(keys[i]), processed.getContentType(), publicForHomepage);
            resultKeys.put(sizeName, key);
            s3.buildPublicUrl(key).ifPresent(u -> resultUrls.put(sizeName, u));
        }
        return new UploadResult(ownerId, imageId, "png", resultKeys, resultUrls);
    }

    private Optional<String> findOriginalKey(String basePath) {
        List<String> keys = s3.listKeysByPrefix(basePath + "original");
        return keys.stream().filter(k -> k.startsWith(basePath + "original"))
                .findFirst();
    }

    private String contentTypeFromExt(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private String extFromContentType(String ct) {
        if (ct == null) return "";
        String lower = ct.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "";
        };
    }

    private String baseProductPath(String productId, String imageId) {
        return "product-images/" + sanitize(productId) + "/" + imageId + "/";
    }

    private String baseTagPath(String tagGroupId, String imageId) {
        return "tag-images/" + sanitize(tagGroupId) + "/" + imageId + "/";
    }

    private String sanitize(String in) {
        if (!StringUtils.hasText(in)) return "unknown";
        return in.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    // ================= User Avatar ==================
    public String getUserAvatarKey(String userId) {
        return "user-avatars/" + sanitize(userId) + "/avatar.png";
    }

    public Map<String, String> uploadUserAvatar(String userId, byte[] bytes, String contentType) throws IOException {
        // Always process to square 512 PNG, store privately
        ImageProcessingService.ProcessedResult processed = imageService.processToPngSquare(bytes);
        byte[] png512 = processed.getImagesBySize().get(ImageProcessingService.SizeKey.S512);
        String key = getUserAvatarKey(userId);
        s3.upload(key, png512, "image/png", false);
        Map<String, String> resp = new HashMap<>();
        resp.put("key", key);
        s3.buildPublicUrl(key).ifPresent(url -> resp.put("publicUrl", url));
        return resp;
    }

    public record UploadResult(String productId, String imageId, String format,
                               Map<String, String> keys, Map<String, String> urls) {
    }
}
