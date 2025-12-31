package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.product.ProductArchiveResponse;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.dto.product.ProductUpdateRequest;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.StorageFile;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.model.product.ProductArchive;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.*;
import kirillzhdanov.identityservice.tenant.ContextGuards;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final GroupTagRepository groupTagRepository;
    private final ProductArchiveRepository productArchiveRepository;
    private final StorageFileRepository storageFileRepository;
    private final S3StorageService s3StorageService;
    private final MediaService mediaService;
    private final PathResolutionService pathResolutionService;

    // ===== Context guards: use centralized helpers =====

    // Common helpers to reduce duplication
    private Brand requireBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Бренд не найден: " + brandId));
    }

    private Product requireProductInContext(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
        ContextGuards.requireEntityBrandMatchesContextOr404(product.getBrand());
        return product;
    }

    private ProductArchiveResponse mapArchive(ProductArchive a) {
        return ProductArchiveResponse.builder()
                .id(a.getId())
                .originalProductId(a.getOriginalProductId())
                .name(a.getName())
                .description(a.getDescription())
                .price(a.getPrice())
                .promoPrice(a.getPromoPrice())
                .brandId(a.getBrandId())
                .groupTagId(a.getGroupTagId())
                .groupPath(a.getGroupPath())
                .visible(a.isVisible())
                .archivedAt(a.getArchivedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private void fillProduct(Product product,
                              String name,
                              String description,
                              java.math.BigDecimal price,
                              java.math.BigDecimal promoPrice,
                              Brand brand,
                              GroupTag groupTag,
                              boolean visible) {
        product.setName(name);
        product.setDescription(description);
        if (price != null) product.setPrice(price);
        product.setPromoPrice(promoPrice);
        product.setBrand(brand);
        product.setGroupTag(groupTag);
        product.setVisible(visible);
    }

    private GroupTag createGroupTag(String name, Brand brand, GroupTag parent) {
        GroupTag created = new GroupTag(name, brand, parent);
        return groupTagRepository.save(created);
    }

    

    private ProductResponse buildResponse(Product product, String variant) {
        String imageUrl = buildProductImageUrl(product, variant);
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .promoPrice(product.getPromoPrice())
                .brandId(product.getBrand().getId())
                .groupTagId(product.getGroupTag() != null ? product.getGroupTag().getId() : null)
                .visible(product.isVisible())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .imageUrl(imageUrl)
                .build();
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        // Приватные операции: бренд из запроса должен совпадать с активным контекстом
        ContextGuards.requireBrandInContextOr404(request.getBrandId());

        Brand brand = requireBrand(request.getBrandId());

        GroupTag groupTag = null;
        if (request.getGroupTagId() != null && request.getGroupTagId() != 0) {
            groupTag = groupTagRepository.findById(request.getGroupTagId())
                    .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена: " + request.getGroupTagId()));
            // проверим, что группа принадлежит бренду
            if (!groupTag.getBrand().getId().equals(brand.getId())) {
                throw new IllegalArgumentException("Группа принадлежит другому бренду");
            }
        }

        Product product = new Product();
        fillProduct(
                product,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getPromoPrice(),
                brand,
                groupTag,
                request.isVisible()
        );

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    // Восстанавливает (find-or-create) цепочку групп по именам внутри бренда и возвращает последнюю группу
    private GroupTag ensurePathByNames(Brand brand, java.util.List<String> names) {
        GroupTag currentParent = null;
        for (String rawName : names) {
            String name = (rawName == null) ? "" : rawName.trim();
            if (name.isBlank()) continue;
            java.util.Optional<GroupTag> found = groupTagRepository.findByBrandAndNameAndParent(brand, name, currentParent);
            if (found.isPresent()) {
                currentParent = found.get();
            } else {
                currentParent = createGroupTag(name, brand, currentParent);
            }
        }
        return currentParent;
    }

    // Делегат: восстановление родителей по пути (включая лист), без автосоздания по имени
    private GroupTag ensureParentsArchiveFirst(Brand brand, String path) {
        return pathResolutionService.ensureParentsArchiveFullNoCreate(brand, path);
    }

    @Transactional
    public ProductResponse updateVisibility(Long productId, boolean visible) {
        Product product = requireProductInContext(productId);
        product.setVisible(visible);
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public List<ProductResponse> getByBrandAndGroup(Long brandId, Long groupTagId, boolean visibleOnly) {
        // Жёстко ограничиваем доступом по контексту бренда
        ContextGuards.requireBrandInContextOr404(brandId);
        Brand brand = requireBrand(brandId);

        List<Product> products;
        if (groupTagId == null || groupTagId == 0) {
            products = visibleOnly
                    ? productRepository.findByBrandAndGroupTagIsNullAndVisibleIsTrue(brand)
                    : productRepository.findByBrandAndGroupTagIsNull(brand);
        } else {
            products = visibleOnly
                    ? productRepository.findByBrandAndGroupTagIdAndVisibleIsTrue(brand, groupTagId)
                    : productRepository.findByBrandAndGroupTagId(brand, groupTagId);
        }

        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Публичная версия: товары по бренду и группе, всегда только видимые, без проверки tenant-контекста.
     * Используется MenuController для публичного меню.
     */
    @Transactional
    public List<ProductResponse> getPublicByBrandAndGroup(Long brandId, Long groupTagId) {
        Brand brand = requireBrand(brandId);

        List<Product> products;
        if (groupTagId == null || groupTagId == 0) {
            products = productRepository.findByBrandAndGroupTagIsNullAndVisibleIsTrue(brand);
        } else {
            products = productRepository.findByBrandAndGroupTagIdAndVisibleIsTrue(brand, groupTagId);
        }

        return products.stream().map(this::toResponsePublic).collect(Collectors.toList());
    }

    private ProductResponse toResponse(Product product) {
        return buildResponse(product, "H512");
    }

    // Публичная версия ответа: используем H256 (16:9 высота 256)
    private ProductResponse toResponsePublic(Product product) {
        return buildResponse(product, "H256");
    }

    private String buildProductImageUrl(Product product, String variant) {
        try {
            java.util.List<StorageFile> files = storageFileRepository.findByOwnerTypeAndOwnerId("PRODUCT", product.getId());
            String useVariant = (variant == null || variant.isBlank()) ? "H512" : variant;
            java.util.Optional<StorageFile> img = files.stream()
                    .filter(f -> "PRODUCT_IMAGE".equals(f.getPurpose()) && useVariant.equals(f.getUsageType()))
                    .findFirst();
            return img.flatMap(storageFile -> s3StorageService.buildPresignedGetUrl(storageFile.getPath(), java.time.Duration.ofDays(7))).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public ProductResponse uploadImage(Long productId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Пустой файл изображения");
        }
        Product product = requireProductInContext(productId);

        String pid = String.valueOf(product.getId());
        var res = mediaService.uploadProductImage(pid, file.getBytes(), file.getContentType());

        // upsert по вариантам ORIGINAL/H256/H512
        upsertProductImageFile(product, "ORIGINAL", res.get("ORIGINAL"));
        upsertProductImageFile(product, "H256", res.get("H256"));
        upsertProductImageFile(product, "H512", res.get("H512"));

        return toResponse(product);
    }

    private void upsertProductImageFile(Product product, String usage, String path) {
        if (path == null) return;
        java.util.List<StorageFile> files = storageFileRepository.findByOwnerTypeAndOwnerId("PRODUCT", product.getId());
        java.util.Optional<StorageFile> existing = files.stream()
                .filter(f -> "PRODUCT_IMAGE".equals(f.getPurpose()) && usage.equals(f.getUsageType()))
                .findFirst();
        StorageFile sf = existing.orElseGet(() -> StorageFile.builder()
                .ownerType("PRODUCT")
                .ownerId(product.getId())
                .purpose("PRODUCT_IMAGE")
                .usageType(usage)
                .createdAt(LocalDateTime.now())
                .build());
        sf.setPath(path);
        sf.setUpdatedAt(LocalDateTime.now());
        storageFileRepository.save(sf);
    }

    @Transactional
    public void deleteToArchive(Long productId) {
        Product product = requireProductInContext(productId);

        ProductArchive archive = new ProductArchive();
        archive.setOriginalProductId(product.getId());
        archive.setName(product.getName());
        archive.setDescription(product.getDescription());
        archive.setPrice(product.getPrice());
        archive.setPromoPrice(product.getPromoPrice());
        archive.setBrandId(product.getBrand().getId());
        archive.setGroupTagId(product.getGroupTag() != null ? product.getGroupTag().getId() : null);
        // Сохраняем человеко-читаемый путь из названий (Бренд/Родитель/Дочерний/...)
        String namePath = buildNamePath(product.getBrand(), product.getGroupTag());
        archive.setGroupPath(namePath);
        archive.setVisible(product.isVisible());
        archive.setArchivedAt(LocalDateTime.now());
        // переносим исходные временные метки товара
        archive.setCreatedAt(product.getCreatedAt());
        archive.setUpdatedAt(product.getUpdatedAt());

        productArchiveRepository.save(archive);
        productRepository.delete(product);
    }

    // Формирует путь вида "/Brand/Parent/Child/" из названий бренда и иерархии групп
    private String buildNamePath(Brand brand, GroupTag leaf) {
        java.util.LinkedList<String> parts = new java.util.LinkedList<>();
        if (leaf != null) {
            GroupTag cur = leaf;
            while (cur != null) {
                parts.addFirst(safeName(cur.getName()));
                cur = cur.getParent();
            }
        }
        parts.addFirst(safeName(brand != null ? brand.getName() : ""));
        return "/" + String.join("/", parts) + "/";
    }

    private String safeName(String s) {
        if (s == null) return "";
        // убираем разделители, чтобы не ломать вид пути
        return s.replace("/", "-");
    }

    @Transactional
    public ProductResponse move(Long productId, Long targetGroupTagId) {
        Product product = requireProductInContext(productId);
        Brand brand = product.getBrand();

        if (targetGroupTagId == null || targetGroupTagId == 0) {
            product.setGroupTag(null);
        } else {
            GroupTag target = groupTagRepository.findById(targetGroupTagId)
                    .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена: " + targetGroupTagId));
            if (!target.getBrand().getId().equals(brand.getId())) {
                throw new IllegalArgumentException("Целевая группа принадлежит другому бренду");
            }
            product.setGroupTag(target);
        }

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse getById(Long productId) {
        // Для приватных ручек требуем соответствие бренда товара контексту
        Product product = requireProductInContext(productId);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long productId, ProductUpdateRequest request) {
        Product product = requireProductInContext(productId);

        if (request.getName() != null) product.setName(request.getName());
        product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        product.setPromoPrice(request.getPromoPrice());
        if (request.getVisible() != null) product.setVisible(request.getVisible());

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse changeBrand(Long productId, Long brandId) {
        Product product = requireProductInContext(productId);
        // Разрешаем смену бренда только из контекста ИСХОДНОГО бренда товара.
        ContextGuards.requireEntityBrandMatchesContextOr404(product.getBrand());
        Brand newBrand = requireBrand(brandId);

        // Если текущая группа не относится к новому бренду — сбрасываем в корень
        if (product.getGroupTag() != null) {
            GroupTag gt = product.getGroupTag();
            if (!gt.getBrand().getId().equals(newBrand.getId())) {
                product.setGroupTag(null);
            }
        }
        product.setBrand(newBrand);
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public List<ProductArchiveResponse> listArchiveByBrand(Long brandId) {
        ContextGuards.requireBrandInContextOr404(brandId);
        return productArchiveRepository.findByBrandId(brandId)
                .stream()
                .map(this::mapArchive)
                .collect(Collectors.toList());
    }

    @Transactional
    public Page<ProductArchiveResponse> listArchiveByBrandPaged(Long brandId, org.springframework.data.domain.Pageable pageable) {
        ContextGuards.requireBrandInContextOr404(brandId);
        var page = productArchiveRepository.findByBrandId(brandId, pageable);
        return page.map(this::mapArchive);
    }

    @Transactional
    public ProductResponse restoreFromArchive(Long archiveId, Long targetGroupTagId) {
        ProductArchive archive = productArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись архива не найдена: " + archiveId));
        // Восстановление возможно только в рамках текущего контекста бренда
        ContextGuards.requireBrandInContextOr404(archive.getBrandId());
        Brand brand = requireBrand(archive.getBrandId());

        GroupTag groupTag = null;
        Long sourceGroupId = targetGroupTagId != null ? targetGroupTagId : archive.getGroupTagId();
        if (sourceGroupId != null && sourceGroupId != 0) {
            groupTag = groupTagRepository.findById(sourceGroupId).orElse(null);
            if (groupTag != null && !groupTag.getBrand().getId().equals(brand.getId())) {
                groupTag = null;
            }
        }

        // Если группу не нашли, пробуем восстановить цепочку родителей по стратегии: архив-сначала, затем имена
        if (groupTag == null) {
            String path = archive.getGroupPath();
            groupTag = ensureParentsArchiveFirst(brand, path);
            if (groupTag == null && path != null && !path.isBlank()) {
                java.util.List<String> names = pathResolutionService.extractNamesFromPath(path);
                if (!names.isEmpty()) {
                    groupTag = ensurePathByNames(brand, names);
                }
            }
        }

        Product product = new Product();
        fillProduct(
                product,
                archive.getName(),
                archive.getDescription(),
                archive.getPrice(),
                archive.getPromoPrice(),
                brand,
                groupTag,
                archive.isVisible()
        );
        // createdAt/updatedAt выставятся через @PrePersist

        Product saved = productRepository.save(product);
        productArchiveRepository.delete(archive);
        return toResponse(saved);
    }

    @Transactional
    public void deleteArchive(Long archiveId) {
        ProductArchive archive = productArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись архива не найдена: " + archiveId));
        productArchiveRepository.delete(archive);
    }

    @Transactional
    public long purgeArchive(int olderThanDays) {
        int days = olderThanDays <= 0 ? 90 : olderThanDays;
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return productArchiveRepository.deleteByArchivedAtBefore(threshold);
    }
}
