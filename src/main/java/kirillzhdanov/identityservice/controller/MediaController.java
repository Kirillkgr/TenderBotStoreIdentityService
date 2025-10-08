package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import kirillzhdanov.identityservice.dto.ProductImageRef;
import kirillzhdanov.identityservice.dto.TagImageRef;
import kirillzhdanov.identityservice.service.ImageProcessingService;
import kirillzhdanov.identityservice.service.MediaService;
import kirillzhdanov.identityservice.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final ImageProcessingService imageService;
    private final S3StorageService s3;
    private final MediaService mediaService;

    @PostMapping("/upload")
    @Operation(summary = "Загрузка изображения товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN для управления медиа каталога.")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productId") String productId,
            @RequestParam(value = "publicForHomepage", required = false, defaultValue = "false") boolean publicForHomepage
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 10MB)"));
        }

        String contentType = file.getContentType();
        byte[] bytes = file.getBytes();

        var res = mediaService.uploadProduct(bytes, contentType, productId, publicForHomepage);
        return ResponseEntity.ok(Map.of(
                "keys", res.keys(),
                "urls", res.urls(),
                "imageId", res.imageId(),
                "productId", res.productId(),
                "format", res.format()
        ));
    }

    private String sanitize(String in) {
        if (!StringUtils.hasText(in)) return "unknown";
        return in.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private String extFromContentType(String ct) {
        if (ct == null) return "";
        return switch (ct.toLowerCase()) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "";
        };
    }

    // New endpoints for overwrite/delete/regenerate and tag groups

    @PostMapping("/product/overwrite")
    @Operation(summary = "Перезагрузка изображения товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<?> overwriteProduct(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productId") String productId,
            @RequestParam("imageId") String imageId,
            @RequestParam(value = "publicForHomepage", required = false, defaultValue = "false") boolean publicForHomepage
    ) throws IOException {
        byte[] bytes = file.getBytes();
        var res = mediaService.overwriteProduct(bytes, file.getContentType(), productId, imageId, publicForHomepage);
        return ResponseEntity.ok(Map.of(
                "keys", res.keys(),
                "urls", res.urls(),
                "imageId", res.imageId(),
                "productId", res.productId(),
                "format", res.format()
        ));
    }

    @DeleteMapping("/product/derived")
    @Operation(summary = "Удаление производных изображений товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Void> deleteProductDerived(@RequestParam String productId, @RequestParam String imageId) {
        mediaService.deleteDerivedProduct(productId, imageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/product/regenerate")
    @Operation(summary = "Регенерация производных изображений товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<?> regenerateProduct(@RequestParam String productId,
                                               @RequestParam String imageId,
                                               @RequestParam(value = "publicForHomepage", defaultValue = "false") boolean publicForHomepage) throws IOException {
        var res = mediaService.regenerateProductFromOriginal(productId, imageId, publicForHomepage);
        return ResponseEntity.ok(Map.of(
                "keys", res.keys(),
                "urls", res.urls(),
                "imageId", res.imageId(),
                "productId", res.productId(),
                "format", res.format()
        ));
    }

    @DeleteMapping("/product/hard")
    @Operation(summary = "Полное удаление изображения товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Void> hardDeleteProduct(@RequestParam String productId, @RequestParam String imageId) {
        mediaService.hardDeleteProduct(productId, imageId);
        return ResponseEntity.ok().build();
    }

    // Tag group variants
    @PostMapping("/tag/upload")
    @Operation(summary = "Загрузка изображения группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<?> uploadTag(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tagGroupId") String tagGroupId,
            @RequestParam(value = "publicForHomepage", required = false, defaultValue = "false") boolean publicForHomepage
    ) throws IOException {
        var res = mediaService.uploadTagGroup(file.getBytes(), file.getContentType(), tagGroupId, publicForHomepage);
        return ResponseEntity.ok(Map.of(
                "keys", res.keys(),
                "urls", res.urls(),
                "imageId", res.imageId(),
                "tagGroupId", tagGroupId,
                "format", res.format()
        ));
    }

    @PostMapping("/tag/overwrite")
    @Operation(summary = "Перезагрузка изображения группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<?> overwriteTag(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tagGroupId") String tagGroupId,
            @RequestParam("imageId") String imageId,
            @RequestParam(value = "publicForHomepage", required = false, defaultValue = "false") boolean publicForHomepage
    ) throws IOException {
        var res = mediaService.overwriteTagGroup(file.getBytes(), file.getContentType(), tagGroupId, imageId, publicForHomepage);
        return ResponseEntity.ok(Map.of(
                "keys", res.keys(),
                "urls", res.urls(),
                "imageId", res.imageId(),
                "tagGroupId", tagGroupId,
                "format", res.format()
        ));
    }

    @DeleteMapping("/tag/derived")
    @Operation(summary = "Удаление производных изображений группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Void> deleteTagDerived(@RequestParam String tagGroupId, @RequestParam String imageId) {
        mediaService.deleteDerivedTagGroup(tagGroupId, imageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tag/regenerate")
    @Operation(summary = "Регенерация производных изображений группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<?> regenerateTag(@RequestParam String tagGroupId,
                                           @RequestParam String imageId,
                                           @RequestParam(value = "publicForHomepage", defaultValue = "false") boolean publicForHomepage) throws IOException {
        var res = mediaService.regenerateTagGroupFromOriginal(tagGroupId, imageId, publicForHomepage);
        return ResponseEntity.ok(Map.of(
                "keys", res.keys(),
                "urls", res.urls(),
                "imageId", res.imageId(),
                "tagGroupId", tagGroupId,
                "format", res.format()
        ));
    }

    @DeleteMapping("/tag/hard")
    @Operation(summary = "Полное удаление изображения группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Void> hardDeleteTag(@RequestParam String tagGroupId, @RequestParam String imageId) {
        mediaService.hardDeleteTagGroup(tagGroupId, imageId);
        return ResponseEntity.ok().build();
    }

    // ==================== BATCH endpoints ====================

    @PostMapping("/product/derived-batch")
    @Operation(summary = "Batch: удалить производные изображения товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Map<String, Object>> deleteProductDerivedBatch(@RequestBody List<ProductImageRef> refs) {
        int count = 0;
        for (ProductImageRef ref : refs) {
            mediaService.deleteDerivedProduct(ref.getProductId(), ref.getImageId());
            count++;
        }
        return ResponseEntity.ok(Map.of("deletedDerived", count));
    }

    @PostMapping("/product/regenerate-batch")
    @Operation(summary = "Batch: регенерация производных изображений товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Map<String, Object>> regenerateProductBatch(@RequestBody List<ProductImageRef> refs,
                                                                      @RequestParam(value = "publicForHomepage", defaultValue = "false") boolean publicForHomepage) throws java.io.IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        for (ProductImageRef ref : refs) {
            var res = mediaService.regenerateProductFromOriginal(ref.getProductId(), ref.getImageId(), publicForHomepage);
            results.add(Map.of(
                    "productId", res.productId(),
                    "imageId", res.imageId(),
                    "keys", res.keys(),
                    "urls", res.urls(),
                    "format", res.format()
            ));
        }
        return ResponseEntity.ok(Map.of("results", results));
    }

    @PostMapping("/product/hard-batch")
    @Operation(summary = "Batch: полное удаление изображений товара",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Map<String, Object>> hardDeleteProductBatch(@RequestBody List<ProductImageRef> refs) {
        int count = 0;
        for (ProductImageRef ref : refs) {
            mediaService.hardDeleteProduct(ref.getProductId(), ref.getImageId());
            count++;
        }
        return ResponseEntity.ok(Map.of("hardDeleted", count));
    }

    @PostMapping("/tag/derived-batch")
    @Operation(summary = "Batch: удалить производные изображения группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Map<String, Object>> deleteTagDerivedBatch(@RequestBody List<TagImageRef> refs) {
        int count = 0;
        for (TagImageRef ref : refs) {
            mediaService.deleteDerivedTagGroup(ref.getTagGroupId(), ref.getImageId());
            count++;
        }
        return ResponseEntity.ok(Map.of("deletedDerived", count));
    }

    @PostMapping("/tag/regenerate-batch")
    @Operation(summary = "Batch: регенерация производных изображений группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Map<String, Object>> regenerateTagBatch(@RequestBody List<TagImageRef> refs,
                                                                  @RequestParam(value = "publicForHomepage", defaultValue = "false") boolean publicForHomepage) throws java.io.IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        for (TagImageRef ref : refs) {
            var res = mediaService.regenerateTagGroupFromOriginal(ref.getTagGroupId(), ref.getImageId(), publicForHomepage);
            results.add(Map.of(
                    "tagGroupId", ref.getTagGroupId(),
                    "imageId", res.imageId(),
                    "keys", res.keys(),
                    "urls", res.urls(),
                    "format", res.format()
            ));
        }
        return ResponseEntity.ok(Map.of("results", results));
    }

    @PostMapping("/tag/hard-batch")
    @Operation(summary = "Batch: полное удаление изображений группы тегов",
            description = "Требуется аутентификация. Рекомендуется роль OWNER/ADMIN.")
    public ResponseEntity<Map<String, Object>> hardDeleteTagBatch(@RequestBody List<TagImageRef> refs) {
        int count = 0;
        for (TagImageRef ref : refs) {
            mediaService.hardDeleteTagGroup(ref.getTagGroupId(), ref.getImageId());
            count++;
        }
        return ResponseEntity.ok(Map.of("hardDeleted", count));
    }
}
