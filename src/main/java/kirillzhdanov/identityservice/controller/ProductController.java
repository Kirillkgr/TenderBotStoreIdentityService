package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Создание товара в корне бренда или внутри выбранного тега
     */
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Получение товаров по бренду и (опционально) по группе.
     * Если groupTagId = 0 или не задан, возвращаются товары в "корне" бренда.
     * visibleOnly по умолчанию true – возвращать только видимые товары.
     */
    @GetMapping("/by-brand/{brandId}")
    public ResponseEntity<List<ProductResponse>> getByBrandAndGroup(
            @PathVariable Long brandId,
            @RequestParam(required = false) Long groupTagId,
            @RequestParam(defaultValue = "true") boolean visibleOnly
    ) {
        List<ProductResponse> response = productService.getByBrandAndGroup(brandId, groupTagId, visibleOnly);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление флага видимости товара
     */
    @PatchMapping("/{productId}/visibility")
    public ResponseEntity<ProductResponse> updateVisibility(
            @PathVariable Long productId,
            @RequestParam @NotNull Boolean visible
    ) {
        ProductResponse response = productService.updateVisibility(productId, visible);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление товара в архив (мягкое удаление для пользователя). В будущем архив будет чиститься автоматически.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteToArchive(@PathVariable Long productId) {
        productService.deleteToArchive(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Перемещение товара между группами (или в корень бренда при targetGroupTagId = 0)
     */
    @PatchMapping("/{productId}/move")
    public ResponseEntity<ProductResponse> move(
            @PathVariable Long productId,
            @RequestParam(required = false) Long targetGroupTagId
    ) {
        ProductResponse response = productService.move(productId, targetGroupTagId);
        return ResponseEntity.ok(response);
    }
}
