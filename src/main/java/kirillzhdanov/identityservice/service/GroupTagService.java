package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.group.*;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.model.tags.GroupTagArchive;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagArchiveRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupTagService {

    private final GroupTagRepository groupTagRepository;
    private final BrandRepository brandRepository;
    private final GroupTagArchiveRepository groupTagArchiveRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public GroupTagResponse createGroupTag(CreateGroupTagRequest request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));

        GroupTag parent = null;
        if (request.getParentId() != null && request.getParentId() != 0) {
            parent = groupTagRepository.findByIdAndBrand(request.getParentId(), brand)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group tag not found with id: " + request.getParentId()));
        }
        // Check if group tag with same name already exists under the same parent
        if (groupTagRepository.existsByBrandAndNameAndParent(brand, request.getName(), parent)) {
            throw new IllegalArgumentException("Group tag with this name already exists in the specified location");
        }

        GroupTag groupTag = new GroupTag(request.getName(), brand, parent);
        groupTag = groupTagRepository.save(groupTag);

        return convertToDto(groupTag);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public java.util.List<GroupTagTreeResponse> tree(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        java.util.List<GroupTag> all = groupTagRepository.findAllByBrandOrdered(brand);

        java.util.Map<Long, GroupTagTreeResponse> map = new java.util.HashMap<>();
        for (GroupTag gt : all) {
            map.put(gt.getId(), new GroupTagTreeResponse(
                    gt.getId(), gt.getName(), brand.getId(),
                    gt.getParent() != null ? gt.getParent().getId() : null,
                    gt.getLevel()
            ));
        }

        java.util.List<GroupTagTreeResponse> roots = new java.util.ArrayList<>();
        for (GroupTag gt : all) {
            GroupTagTreeResponse dto = map.get(gt.getId());
            Long pid = gt.getParent() != null ? gt.getParent().getId() : null;
            if (pid == null || pid == 0L) {
                roots.add(dto);
            } else {
                GroupTagTreeResponse parent = map.get(pid);
                if (parent != null) parent.getChildren().add(dto);
                else roots.add(dto); // на случай неконсистентности
            }
        }

        return roots;
    }

    public List<GroupTagResponse> getGroupTagsByBrandAndParent(Long brandId, Long parentId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        List<GroupTag> groupTags;
        if (parentId == null || parentId == 0) {
            groupTags = groupTagRepository.findByBrandAndParentIsNull(brand);
        } else {
            groupTags = groupTagRepository.findByBrandAndParentId(brand, parentId);
        }

        return groupTags.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<GroupTagResponse> getGroupTagsTree(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        List<GroupTag> rootGroups = groupTagRepository.findByBrandAndParentIsNull(brand);
        return rootGroups.stream()
                .map(this::convertToDtoWithChildren)
                .collect(Collectors.toList());
    }

    public Page<GroupTagResponse> getGroupTagsPaged(Long brandId, Long parentId, Pageable pageable) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        Page<GroupTag> page;
        if (parentId == null || parentId == 0) {
            page = groupTagRepository.findByBrandAndParentIsNull(brand, pageable);
        } else {
            page = groupTagRepository.findByBrandAndParentId(brand, parentId, pageable);
        }
        return page.map(this::convertToDto);
    }

    @Transactional
    public GroupTagResponse rename(Long groupTagId, String newName) {
        GroupTag groupTag = groupTagRepository.findById(groupTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Group tag not found: " + groupTagId));
        // uniqueness within same parent and brand
        if (groupTagRepository.existsByBrandAndNameAndParent(groupTag.getBrand(), newName, groupTag.getParent())) {
            throw new IllegalArgumentException("Group tag with this name already exists in the specified location");
        }
        groupTag.setName(newName);
        GroupTag saved = groupTagRepository.save(groupTag);
        return convertToDto(saved);
    }

    @Transactional
    public GroupTagResponse updateGroupTag(Long groupTagId, UpdateGroupTagRequest request) {
        GroupTag current = groupTagRepository.findById(groupTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Group tag not found: " + groupTagId));

        // 1) Change brand if requested and different
        if (request.getBrandId() != null && !request.getBrandId().equals(current.getBrand().getId())) {
            changeBrand(groupTagId, request.getBrandId());
            // reload after brand change
            current = groupTagRepository.findById(groupTagId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group tag not found after brand change: " + groupTagId));
        }

        // 2) Move if requested (including to root when parentId = 0)
        if (request.getParentId() != null) {
            long desiredParent = request.getParentId();
            long currentParentId = current.getParent() != null ? current.getParent().getId() : 0L;
            if (desiredParent != currentParentId) {
                move(groupTagId, desiredParent == 0 ? null : desiredParent);
                current = groupTagRepository.findById(groupTagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Group tag not found after move: " + groupTagId));
            }
        }

        // 3) Rename if requested and changed
        if (request.getName() != null && !request.getName().isBlank() && !request.getName().equals(current.getName())) {
            return rename(groupTagId, request.getName());
        }

        return convertToDto(current);
    }

    @Transactional
    public GroupTagResponse changeBrand(Long groupTagId, Long newBrandId) {
        GroupTag root = groupTagRepository.findById(groupTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Group tag not found: " + groupTagId));
        Brand newBrand = brandRepository.findById(newBrandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + newBrandId));
        Brand oldBrand = root.getBrand();

        // Collect subtree including root
        String subtreePrefix = root.getPath() + root.getId() + "/";
        List<GroupTag> subtree = new java.util.ArrayList<>();
        subtree.add(root);
        subtree.addAll(groupTagRepository.findSubtreeByPathPrefix(oldBrand, subtreePrefix));

        // Move all nodes to new brand; detach parent for root (moves subtree accordingly via entity logic)
        root.setParent(null);
        root.setBrand(newBrand);
        groupTagRepository.save(root);

        // For children: just set brand; their path/level recalculation relies on entity logic if needed
        for (GroupTag gt : subtree) {
            if (gt.getId().equals(root.getId())) continue;
            gt.setBrand(newBrand);
            groupTagRepository.save(gt);
        }

        // Now move products under each group to the new brand too (select by OLD brand)
        for (GroupTag gt : subtree) {
            List<kirillzhdanov.identityservice.model.product.Product> prods =
                    productRepository.findByBrandAndGroupTagId(oldBrand, gt.getId());
            for (kirillzhdanov.identityservice.model.product.Product p : prods) {
                // Use service to apply business rules (keeps groupTag if same brand)
                productService.changeBrand(p.getId(), newBrand.getId());
            }
        }

        return convertToDto(root);
    }

    @Transactional
    public GroupTagResponse move(Long groupTagId, Long newParentId) {
        GroupTag groupTag = groupTagRepository.findById(groupTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Group tag not found: " + groupTagId));

        GroupTag newParent = null;
        if (newParentId != null && newParentId != 0) {
            newParent = groupTagRepository.findById(newParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group not found: " + newParentId));
            if (!newParent.getBrand().getId().equals(groupTag.getBrand().getId())) {
                throw new IllegalArgumentException("Parent belongs to different brand");
            }
        }

        // Check uniqueness under new parent
        if (groupTagRepository.existsByBrandAndNameAndParent(groupTag.getBrand(), groupTag.getName(), newParent)) {
            throw new IllegalArgumentException("Group tag with this name already exists in the target location");
        }

        groupTag.setParent(newParent); // updates path/level and children via entity logic
        GroupTag saved = groupTagRepository.save(groupTag);
        return convertToDto(saved);
    }

    public List<GroupTagResponse> breadcrumbs(Long groupTagId) {
        GroupTag node = groupTagRepository.findById(groupTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Group tag not found: " + groupTagId));
        List<GroupTagResponse> result = new ArrayList<>();
        GroupTag cur = node;
        while (cur != null) {
            result.addFirst(convertToDto(cur));
            cur = cur.getParent();
        }
        return result;
    }

    @Transactional
    public void deleteWithArchive(Long groupTagId) {
        GroupTag root = groupTagRepository.findById(groupTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Group tag not found: " + groupTagId));

        Brand brand = root.getBrand();
        String subtreePrefix = root.getPath() + root.getId() + "/";

        // Collect all subtree nodes including root; ensure children first (by level desc)
        List<GroupTag> subtree = new ArrayList<>();
        subtree.add(root);
        subtree.addAll(groupTagRepository.findSubtreeByPathPrefix(brand, subtreePrefix));
        subtree.sort(Comparator.comparingInt(GroupTag::getLevel).reversed());

        // Archive products for each group in subtree
        for (GroupTag gt : subtree) {
            List<kirillzhdanov.identityservice.model.product.Product> prods =
                    productRepository.findByBrandAndGroupTagId(brand, gt.getId());
            for (kirillzhdanov.identityservice.model.product.Product p : prods) {
                productService.deleteToArchive(p.getId());
            }
        }

        // Archive group tags (snapshot) bottom-up
        LocalDateTime now = LocalDateTime.now();
        for (GroupTag gt : subtree) {
            GroupTagArchive a = new GroupTagArchive();
            a.setOriginalGroupTagId(gt.getId());
            a.setBrandId(brand.getId());
            a.setParentId(gt.getParent() != null ? gt.getParent().getId() : null);
            a.setName(gt.getName());
            // человеко-читаемый путь по названиям (Бренд/Родитель/Дочерний/...)
            a.setPath(buildNamePath(brand, gt));
            a.setLevel(gt.getLevel());
            a.setArchivedAt(now);
            groupTagArchiveRepository.save(a);
        }

        // Finally delete root; children will be removed via cascade
        groupTagRepository.delete(root);
    }

    @Transactional
    public long purgeArchive(int olderThanDays) {
        int days = olderThanDays <= 0 ? 90 : olderThanDays;
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        // 1) Удаление явным JPQL delete
        int deleted = groupTagArchiveRepository.deleteAllOlderThan(threshold);
        // 2) Верификация отдельным запросом (что ничего старше порога не осталось)
        long remaining = groupTagArchiveRepository.countByArchivedAtBefore(threshold);
        if (remaining > 0) {
            // Это не ошибка в большинстве БД, но оставим логическую гарантию для тестов
            // Либо можно бросить исключение, если необходимо строгое соответствие
            throw new IllegalStateException("Archive purge verification failed: remaining=" + remaining);
        }
        return deleted;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<GroupTagArchiveResponse> listArchiveByBrand(Long brandId) {
        List<GroupTagArchive> list = groupTagArchiveRepository.findByBrandId(brandId);
        return list.stream()
                .sorted(Comparator.comparing(GroupTagArchive::getArchivedAt).reversed())
                .map(a -> GroupTagArchiveResponse.builder()
                        .id(a.getId())
                        .originalGroupTagId(a.getOriginalGroupTagId())
                        .brandId(a.getBrandId())
                        .parentId(a.getParentId())
                        .name(a.getName())
                        .path(a.getPath())
                        .level(a.getLevel())
                        .archivedAt(a.getArchivedAt())
                        .build())
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public org.springframework.data.domain.Page<GroupTagArchiveResponse> listArchiveByBrandPaged(Long brandId, org.springframework.data.domain.Pageable pageable) {
        var page = groupTagArchiveRepository.findByBrandId(brandId, pageable);
        return page.map(a -> GroupTagArchiveResponse.builder()
                .id(a.getId())
                .originalGroupTagId(a.getOriginalGroupTagId())
                .brandId(a.getBrandId())
                .parentId(a.getParentId())
                .name(a.getName())
                .path(a.getPath())
                .level(a.getLevel())
                .archivedAt(a.getArchivedAt())
                .build());
    }

    @Transactional
    public GroupTagResponse restoreGroupFromArchive(Long archiveId, Long targetParentId) {
        GroupTagArchive a = groupTagArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("GroupTag archive not found: " + archiveId));

        Brand brand = brandRepository.findById(a.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + a.getBrandId()));

        GroupTag parent = null;
        Long parentIdToUse = targetParentId != null ? targetParentId : a.getParentId();
        if (parentIdToUse != null && parentIdToUse != 0) {
            // Если указали конкретный parentId — пытаемся найти. Если не нашли или бренд не совпадает — позже попробуем по path.
            parent = groupTagRepository.findById(parentIdToUse)
                    .orElse(null);
            if (parent != null && !parent.getBrand().getId().equals(brand.getId())) {
                parent = null;
            }
        }

        // Если родитель не найден, сначала пробуем восстановить цепочку родителей из архива по path; если нет записей — по именам
        if (parent == null) {
            String path = a.getPath();
            parent = ensureParentsArchiveFirst(brand, path);
        }

        GroupTag restored = new GroupTag(a.getName(), brand, parent);
        restored = groupTagRepository.save(restored);

        groupTagArchiveRepository.delete(a);
        return convertToDto(restored);
    }


    // Восстанавливает цепочку родителей по path "/Brand/Parent/Child/" по стратегии: архив-сначала, затем имена.
    // Проходит по родительским сегментам (без самого восстанавливаемого тега),
    // для каждого уровня пытается: 1) найти живую группу; 2) восстановить из архива по точному пути; 3) создать по имени.
    private GroupTag ensureParentsArchiveFirst(Brand brand, String path) {
        if (path == null || path.isBlank()) return null;
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.isBlank()) return null;

        String[] parts = trimmed.split("/");
        if (parts.length < 2) return null; // нет даже бренда

        GroupTag currentParent = null;
        StringBuilder prefix = new StringBuilder("/");
        prefix.append(parts[0]).append("/"); // бренд

        for (int i = 1; i < parts.length - 1; i++) {
            String name = parts[i];
            if (name == null || name.isBlank()) continue;
            // 1) живой узел
            java.util.Optional<GroupTag> found = groupTagRepository.findByBrandAndNameAndParent(brand, name, currentParent);
            if (found.isPresent()) {
                currentParent = found.get();
                prefix.append(name).append("/");
                continue;
            }
            // 2) архивный узел по точному пути
            prefix.append(name).append("/");
            java.util.Optional<GroupTagArchive> archived = groupTagArchiveRepository.findByBrandIdAndPath(brand.getId(), prefix.toString());
            if (archived.isPresent()) {
                GroupTagArchive ga = archived.get();
                GroupTag created = new GroupTag(ga.getName(), brand, currentParent);
                created = groupTagRepository.save(created);
                groupTagArchiveRepository.delete(ga);
                currentParent = created;
            } else {
                // 3) создать по имени
                GroupTag created = new GroupTag(name, brand, currentParent);
                created = groupTagRepository.save(created);
                currentParent = created;
            }
        }
        return currentParent;
    }

    @Transactional
    public void deleteGroupArchive(Long archiveId) {
        GroupTagArchive a = groupTagArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("GroupTag archive not found: " + archiveId));
        groupTagArchiveRepository.delete(a);
    }

    // Формирует путь вида "/Brand/Parent/Child/" по названиям
    private String buildNamePath(Brand brand, GroupTag leaf) {
        java.util.LinkedList<String> parts = new java.util.LinkedList<>();
        GroupTag cur = leaf;
        while (cur != null) {
            parts.addFirst(safeName(cur.getName()));
            cur = cur.getParent();
        }
        parts.addFirst(safeName(brand != null ? brand.getName() : ""));
        return "/" + String.join("/", parts) + "/";
    }

    private String safeName(String s) {
        if (s == null) return "";
        return s.replace("/", "-");
    }

    private GroupTagResponse convertToDto(GroupTag groupTag) {
        return new GroupTagResponse(
                groupTag.getId(),
                groupTag.getName(),
                groupTag.getBrand().getId(),
                groupTag.getParent() != null ? groupTag.getParent().getId() : null,
                groupTag.getLevel()
        );
    }

    private GroupTagResponse convertToDtoWithChildren(GroupTag groupTag) {
        GroupTagResponse dto = convertToDto(groupTag);
        if (groupTag.getChildren() != null && !groupTag.getChildren().isEmpty()) {
            List<GroupTagResponse> children = groupTag.getChildren().stream()
                    .map(this::convertToDtoWithChildren)
                    .collect(Collectors.toList());
            dto.setChildren(children);
        }
        return dto;
    }

    /**
     * Проверяет, есть ли ВИДИМЫЕ товары в поддереве данной группы (включая саму группу).
     * Используется для публичного меню, чтобы скрывать пустые ветки.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public boolean hasVisibleProductsInSubtree(Long brandId, Long groupTagId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        GroupTag node = groupTagRepository.findById(groupTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Group tag not found: " + groupTagId));

        // Сначала проверим саму группу
        if (!productRepository.findByBrandAndGroupTagIdAndVisibleIsTrue(brand, node.getId()).isEmpty()) {
            return true;
        }

        // Затем проверим дочерние группы через path-префикс
        String prefix = node.getPath() + node.getId() + "/";
        List<GroupTag> subtree = groupTagRepository.findSubtreeByPathPrefix(brand, prefix);
        for (GroupTag gt : subtree) {
            if (!productRepository.findByBrandAndGroupTagIdAndVisibleIsTrue(brand, gt.getId()).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
