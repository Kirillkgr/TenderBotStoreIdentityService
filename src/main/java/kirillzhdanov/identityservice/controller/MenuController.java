package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.dto.menu.PublicBrandResponse;
import kirillzhdanov.identityservice.dto.menu.PublicBrandMinResponse;
import kirillzhdanov.identityservice.dto.menu.PublicGroupTagResponse;
import kirillzhdanov.identityservice.dto.menu.PublicProductResponse;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.service.BrandService;
import kirillzhdanov.identityservice.service.GroupTagService;
import kirillzhdanov.identityservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/menu/v1")
@RequiredArgsConstructor
public class MenuController {

    private final BrandService brandService;
    private final GroupTagService groupTagService;
    private final ProductService productService;

    // 1) Публичный список брендов (минимум данных)
    @GetMapping("/brands")
    @Operation(summary = "Публичные бренды", description = "Публично. Возвращает список брендов (минимальная информация).")
    public ResponseEntity<List<PublicBrandResponse>> getBrands() {
        List<BrandDto> brands = brandService.getAllBrandsPublic();
        List<PublicBrandResponse> response = brands.stream()
                .map(b -> new PublicBrandResponse(b.getId(), b.getName(), b.getDomain()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Минимальный список для быстрой проверки сабдомена: id + domain
    @GetMapping("/brands/min")
    @Operation(summary = "Публичные бренды (минимум)", description = "Публично. Быстрый список {id, domain} для проверки сабдомена на фронте.")
    public ResponseEntity<List<PublicBrandMinResponse>> getBrandsMin() {
        return ResponseEntity.ok(brandService.getAllBrandsPublicMin());
    }

    // 2) Публичная карточка бренда (минимум данных)
    @GetMapping("/brands/{brandId}")
    @Operation(summary = "Публичная карточка бренда", description = "Публично. Минимальные поля.")
    public ResponseEntity<PublicBrandResponse> getBrand(@PathVariable Long brandId) {
        BrandDto b = brandService.getBrandByIdPublic(brandId);
        return ResponseEntity.ok(new PublicBrandResponse(b.getId(), b.getName(), b.getDomain()));
    }

    // 3) Публичные теги бренда по родителю (parentId=0 -> корневые)
    @GetMapping("/brands/{brandId}/tags")
    @Operation(summary = "Публичные теги бренда", description = "Публично. Скрывает пустые группы.")
    public ResponseEntity<List<PublicGroupTagResponse>> getBrandTags(
            @PathVariable Long brandId,
            @RequestParam(required = false, defaultValue = "0") Long parentId
    ) {
        List<GroupTagResponse> tags = groupTagService.getGroupTagsByBrandAndParent(brandId, parentId);
        // Публичное меню: скрываем пустые группы (нет видимых товаров ни в самой группе, ни глубже)
        List<PublicGroupTagResponse> response = tags.stream()
                .filter(t -> groupTagService.hasVisibleProductsInSubtree(brandId, t.getId()))
                .map(t -> new PublicGroupTagResponse(t.getId(), t.getName(), t.getParentId(), t.getLevel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 4) Публичные товары бренда по группе (groupTagId=0 -> корневые). Всегда только видимые.
    @GetMapping("/brands/{brandId}/products")
    @Operation(summary = "Публичные товары бренда", description = "Публично. Всегда только видимые товары.")
    public ResponseEntity<List<PublicProductResponse>> getBrandProducts(
            @PathVariable Long brandId,
            @RequestParam(required = false, defaultValue = "0") Long groupTagId
    ) {
        List<ProductResponse> products = productService.getByBrandAndGroup(brandId, groupTagId, true);
        List<PublicProductResponse> response = products.stream()
                .map(p -> new PublicProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getPromoPrice(), p.isVisible()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // ALIAS: совместимость с путём вида /menu/v1/products/by-brand/{brandId}
    @GetMapping("/products/by-brand/{brandId}")
    @Operation(summary = "Alias: публичные товары бренда", description = "Публично. Алиас на /brands/{brandId}/products.")
    public ResponseEntity<List<PublicProductResponse>> getBrandProductsAlias(
            @PathVariable Long brandId,
            @RequestParam(required = false, defaultValue = "0") Long groupTagId
    ) {
        return getBrandProducts(brandId, groupTagId);
    }

    // ALIAS: совместимость с путём вида /menu/v1/tags/by-brand/{brandId}
    @GetMapping("/tags/by-brand/{brandId}")
    @Operation(summary = "Alias: публичные теги бренда", description = "Публично. Алиас на /brands/{brandId}/tags.")
    public ResponseEntity<List<PublicGroupTagResponse>> getBrandTagsAlias(
            @PathVariable Long brandId,
            @RequestParam(required = false, defaultValue = "0") Long parentId
    ) {
        return getBrandTags(brandId, parentId);
    }

    // ALIAS: совместимость с путём вида /menu/v1/group-tags/by-brand/{brandId}
    @GetMapping("/group-tags/by-brand/{brandId}")
    @Operation(summary = "Alias: публичные группы тегов бренда", description = "Публично. Алиас на /brands/{brandId}/tags.")
    public ResponseEntity<List<PublicGroupTagResponse>> getBrandGroupTagsAlias(
            @PathVariable Long brandId,
            @RequestParam(required = false, defaultValue = "0") Long parentId
    ) {
        return getBrandTags(brandId, parentId);
    }
}
