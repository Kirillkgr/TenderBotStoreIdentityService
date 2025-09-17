package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.model.product.ProductArchive;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import kirillzhdanov.identityservice.repository.ProductArchiveRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
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

    @Transactional
    public ProductResponse updateVisibility(Long productId, boolean visible) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));
        product.setVisible(visible);
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public List<ProductResponse> getByBrandAndGroup(Long brandId, Long groupTagId, boolean visibleOnly) {
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
                .build();
    }

    @Transactional
    public void deleteToArchive(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + productId));

        ProductArchive archive = new ProductArchive();
        archive.setOriginalProductId(product.getId());
        archive.setName(product.getName());
        archive.setDescription(product.getDescription());
        archive.setPrice(product.getPrice());
        archive.setPromoPrice(product.getPromoPrice());
        archive.setBrandId(product.getBrand().getId());
        archive.setGroupTagId(product.getGroupTag() != null ? product.getGroupTag().getId() : null);
        String path = "/";
        if (product.getGroupTag() != null) {
            GroupTag gt = product.getGroupTag();
            path = gt.getPath() + gt.getId() + "/";
        }
        archive.setGroupPath(path);
        archive.setVisible(product.isVisible());
        archive.setArchivedAt(LocalDateTime.now());

        productArchiveRepository.save(archive);
        productRepository.delete(product);
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
}
