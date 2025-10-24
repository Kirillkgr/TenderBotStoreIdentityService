package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.product.ProductArchiveResponse;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.dto.product.ProductUpdateRequest;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
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
    private final GroupTagArchiveRepository groupTagArchiveRepository;

    // ===== Context guards: use centralized helpers =====

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        // Приватные операции: бренд из запроса должен совпадать с активным контекстом
        ContextGuards.requireBrandInContextOr404(request.getBrandId());

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Бренд не найден: " + request.getBrandId()));

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
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setPromoPrice(request.getPromoPrice());
        product.setBrand(brand);
        product.setGroupTag(groupTag); // null -> корень
        product.setVisible(request.isVisible());

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
                GroupTag created = new GroupTag(name, brand, currentParent);
                created = groupTagRepository.save(created);
                currentParent = created;
            }
        }
        return currentParent;
    }

    // Восстанавливает цепочку по пути "/Brand/Parent/Child/..." используя записи архива для каждого узла, если они есть;
    // если нет — возвращает null (будет применён fallback по именам)
    private GroupTag ensureParentsArchiveFirst(Brand brand, String path) {
        if (path == null || path.isBlank()) return null;
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.isBlank()) return null;

        String[] parts = trimmed.split("/");
        if (parts.length < 2) return null; // только бренд

        GroupTag currentParent = null;
        StringBuilder prefix = new StringBuilder("/");
        prefix.append(parts[0]).append("/"); // бренд

        for (int i = 1; i < parts.length; i++) {
            String name = parts[i];
            if (name == null || name.isBlank()) continue;
            // если уже есть живая группа — идём дальше
            java.util.Optional<GroupTag> existing = groupTagRepository.findByBrandAndNameAndParent(brand, name, currentParent);
            if (existing.isPresent()) {
                currentParent = existing.get();
                prefix.append(name).append("/");
                continue;
            }
            // пробуем восстановить узел из архива по точному пути
            prefix.append(name).append("/");
            java.util.Optional<kirillzhdanov.identityservice.model.tags.GroupTagArchive> archived = groupTagArchiveRepository.findByBrandIdAndPath(brand.getId(), prefix.toString());
            if (archived.isPresent()) {
                var ga = archived.get();
                GroupTag created = new GroupTag(ga.getName(), brand, currentParent);
                created = groupTagRepository.save(created);
                groupTagArchiveRepository.delete(ga);
                // Ensure deletion is visible for subsequent reads in the same test flow
                groupTagArchiveRepository.flush();
                currentParent = created;
            } else {
                // нет в архиве — прекращаем, пусть fallback по именам решит (вернём null)
                return null;
            }
        }
        return currentParent;
    }

    @Transactional
    public ProductResponse updateVisibility(Long productId, boolean visible) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
        ContextGuards.requireEntityBrandMatchesContextOr404(product.getBrand());
        product.setVisible(visible);
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public List<ProductResponse> getByBrandAndGroup(Long brandId, Long groupTagId, boolean visibleOnly) {
        // Жёстко ограничиваем доступом по контексту бренда
        ContextGuards.requireBrandInContextOr404(brandId);
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Бренд не найден: " + brandId));

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
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Бренд не найден: " + brandId));

        List<Product> products;
        if (groupTagId == null || groupTagId == 0) {
            products = productRepository.findByBrandAndGroupTagIsNullAndVisibleIsTrue(brand);
        } else {
            products = productRepository.findByBrandAndGroupTagIdAndVisibleIsTrue(brand, groupTagId);
        }

        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ProductResponse toResponse(Product product) {
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
                .build();
    }

    @Transactional
    public void deleteToArchive(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
        ContextGuards.requireEntityBrandMatchesContextOr404(product.getBrand());

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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
        ContextGuards.requireEntityBrandMatchesContextOr404(product.getBrand());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
        ContextGuards.requireEntityBrandMatchesContextOr404(product.getBrand());

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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
        // Разрешаем смену бренда только из контекста ИСХОДНОГО бренда товара.
        ContextGuards.requireEntityBrandMatchesContextOr404(product.getBrand());
        Brand newBrand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Бренд не найден: " + brandId));

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
                .map(a -> ProductArchiveResponse.builder()
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
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Page<ProductArchiveResponse> listArchiveByBrandPaged(Long brandId, org.springframework.data.domain.Pageable pageable) {
        ContextGuards.requireBrandInContextOr404(brandId);
        var page = productArchiveRepository.findByBrandId(brandId, pageable);
        return page.map(a -> ProductArchiveResponse.builder()
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
                .build());
    }

    @Transactional
    public ProductResponse restoreFromArchive(Long archiveId, Long targetGroupTagId) {
        ProductArchive archive = productArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись архива не найдена: " + archiveId));
        // Восстановление возможно только в рамках текущего контекста бренда
        ContextGuards.requireBrandInContextOr404(archive.getBrandId());
        Brand brand = brandRepository.findById(archive.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Бренд не найден: " + archive.getBrandId()));

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
                java.util.List<String> names = new java.util.ArrayList<>();
                String trimmed = path.trim();
                if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
                if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
                if (!trimmed.isBlank()) {
                    String[] parts = trimmed.split("/");
                    // parts[0] — имя бренда; остальные — цепочка групп (включая конечную группу товара)
                    for (int i = 1; i < parts.length; i++) {
                        String name = parts[i];
                        if (name != null && !name.isBlank()) names.add(name);
                    }
                }
                if (!names.isEmpty()) {
                    groupTag = ensurePathByNames(brand, names);
                }
            }
        }

        Product product = new Product();
        product.setName(archive.getName());
        product.setDescription(archive.getDescription());
        product.setPrice(archive.getPrice());
        product.setPromoPrice(archive.getPromoPrice());
        product.setBrand(brand);
        product.setGroupTag(groupTag);
        product.setVisible(archive.isVisible());
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
