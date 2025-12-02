package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kirillzhdanov.identityservice.dto.product.ProductArchiveResponse;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.dto.product.ProductUpdateRequest;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final RbacGuard rbacGuard;

    /**
     * Создание товара в корне бренда или внутри выбранного тега
     */
    @PostMapping
    @Operation(summary = "Создать товар", description = "Требования: роль OWNER или ADMIN активного membership. Чужой бренд → 404; недостаточно прав → 403.")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        rbacGuard.requireOwnerOrAdmin();
        ProductResponse response = productService.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Получить карточку товара по id
     */
    @GetMapping("/{productId}")
    @Operation(summary = "Товар по ID", description = "Требования: аутентификация; доступ только к товарам в пределах контекста (чужие → 404). Публичный аналог см. /menu/v1.")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getById(productId));
    }

    /**
     * Получение товаров по бренду и (опционально) по группе.
     * Если groupTagId = 0 или не задан, возвращаются товары в "корне" бренда.
     * visibleOnly по умолчанию true – возвращать только видимые товары.
     */
    @GetMapping("/by-brand/{brandId}")
    @Operation(summary = "Товары бренда (внутренний)", description = "Требования: аутентификация. Для публичного меню используйте /menu/v1/brands/{brandId}/products.")
    public ResponseEntity<List<ProductResponse>> getByBrandAndGroup(
            @PathVariable Long brandId,
            @RequestParam(required = false) Long groupTagId,
            @RequestParam(defaultValue = "true") boolean visibleOnly
    ) {
        List<ProductResponse> response = productService.getByBrandAndGroup(brandId, groupTagId, visibleOnly);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление карточки товара
     */
    @PutMapping("/{productId}")
    @Operation(summary = "Обновить товар", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(productService.update(productId, request));
    }

    /**
     * Обновление флага видимости товара
     */
    @PatchMapping("/{productId}/visibility")
    @Operation(summary = "Сменить видимость товара", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<ProductResponse> updateVisibility(
            @PathVariable Long productId,
            @RequestParam @NotNull Boolean visible
    ) {
        rbacGuard.requireOwnerOrAdmin();
        ProductResponse response = productService.updateVisibility(productId, visible);
        return ResponseEntity.ok(response);
    }

    /**
     * Смена бренда у товара (при смене бренда группа сбрасывается, если относится к другому бренду)
     */
    @PatchMapping("/{productId}/brand")
    @Operation(summary = "Сменить бренд товара", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<ProductResponse> changeBrand(
            @PathVariable Long productId,
            @RequestParam @NotNull Long brandId
    ) {
        rbacGuard.requireOwnerOrAdmin();
        ProductResponse response = productService.changeBrand(productId, brandId);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление товара в архив (мягкое удаление для пользователя). В будущем архив будет чиститься автоматически.
     */
    @DeleteMapping("/{productId}")
    @Operation(summary = "Архивировать товар", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Void> deleteToArchive(@PathVariable Long productId) {
        rbacGuard.requireOwnerOrAdmin();
        productService.deleteToArchive(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Перемещение товара между группами (или в корень бренда при targetGroupTagId = 0)
     */
    @PatchMapping("/{productId}/move")
    @Operation(summary = "Переместить товар", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<ProductResponse> move(
            @PathVariable Long productId,
            @RequestParam(required = false) Long targetGroupTagId
    ) {
        rbacGuard.requireOwnerOrAdmin();
        ProductResponse response = productService.move(productId, targetGroupTagId);
        return ResponseEntity.ok(response);
    }

    /**
     * Список архива по бренду
     */
    @GetMapping("/archive")
    @Operation(summary = "Архив товаров бренда", description = "Требования: аутентификация. Данные в пределах контекста.")
    public ResponseEntity<List<ProductArchiveResponse>> listArchiveByBrand(@RequestParam @NotNull Long brandId) {
        return ResponseEntity.ok(productService.listArchiveByBrand(brandId));
    }

    /**
     * Пагинированный список архива по бренду
     */
    @GetMapping("/archive/paged")
    @Operation(summary = "Архив товаров бренда (пагинация)", description = "Требования: аутентификация. Данные в пределах контекста.")
    public ResponseEntity<Page<ProductArchiveResponse>> listArchiveByBrandPaged(
            @RequestParam @NotNull Long brandId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(productService.listArchiveByBrandPaged(brandId, pageable));
    }

    /**
     * Восстановление из архива в исходную или указанную группу
     */
    @PostMapping("/archive/{archiveId}/restore")
    @Operation(summary = "Восстановить товар из архива", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<ProductResponse> restoreFromArchive(
            @PathVariable Long archiveId,
            @RequestParam(required = false) Long targetGroupTagId
    ) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(productService.restoreFromArchive(archiveId, targetGroupTagId));
    }

    /**
     * Удалить запись из архива
     */
    @DeleteMapping("/archive/{archiveId}")
    @Operation(summary = "Удалить запись архива", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Void> deleteArchive(@PathVariable Long archiveId) {
        rbacGuard.requireOwnerOrAdmin();
        productService.deleteArchive(archiveId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Очистка архива старше N дней (по умолчанию 90)
     */
    @DeleteMapping("/archive/purge")
    @Operation(summary = "Очистить архив", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Long> purgeArchive(@RequestParam(defaultValue = "90") int olderThanDays) {
        rbacGuard.requireOwnerOrAdmin();
        long deleted = productService.purgeArchive(olderThanDays);
        return ResponseEntity.ok(deleted);
    }
}
