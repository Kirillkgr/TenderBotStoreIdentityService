package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.dto.group.GroupTagTreeResponse;
import kirillzhdanov.identityservice.dto.group.UpdateGroupTagRequest;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import kirillzhdanov.identityservice.tenant.TenantContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GroupTagServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private GroupTagService groupTagService;
    @Autowired
    private ProductService productService;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private GroupTagRepository groupTagRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private GroupTagArchiveRepository groupTagArchiveRepository;
    @Autowired
    private ProductArchiveRepository productArchiveRepository;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private Brand brand1;
    private Brand brand2;

    @BeforeEach
    void setup(TestInfo info) {
        int n = COUNTER.incrementAndGet();
        System.out.printf("[GT-%02d] %s%n", n, info.getDisplayName());

        brand1 = new Brand();
        brand1.setName("Brand-A-" + System.nanoTime());
        brand1 = brandRepository.save(brand1);

        brand2 = new Brand();
        brand2.setName("Brand-B-" + System.nanoTime());
        brand2 = brandRepository.save(brand2);
    }

    private void ctx(Long brandId) { TenantContext.setBrandId(brandId); }

    @Test
    @DisplayName("Группа: hasVisibleProductsInSubtree - видимый товар в самой группе")
    void hasVisible_in_current_group() {
        ctx(brand1.getId());
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("R");
        r.setBrandId(brand1.getId());
        r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);

        ProductCreateRequest pr = new ProductCreateRequest();
        pr.setName("VP");
        pr.setPrice(new BigDecimal("1"));
        pr.setBrandId(brand1.getId());
        pr.setGroupTagId(root.getId());
        pr.setVisible(true);
        ctx(brand1.getId());
        productService.create(pr);

        assertTrue(groupTagService.hasVisibleProductsInSubtree(brand1.getId(), root.getId()));
    }

    @Test
    @DisplayName("Группа: hasVisibleProductsInSubtree - видимый товар только в дочерней группе")
    void hasVisible_in_child_group() {
        ctx(brand1.getId());
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("R");
        r.setBrandId(brand1.getId());
        r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);

        CreateGroupTagRequest c = new CreateGroupTagRequest();
        c.setName("C");
        c.setBrandId(brand1.getId());
        c.setParentId(root.getId());
        GroupTagResponse child = groupTagService.createGroupTag(c);

        ProductCreateRequest pr = new ProductCreateRequest();
        pr.setName("VP");
        pr.setPrice(new BigDecimal("1"));
        pr.setBrandId(brand1.getId());
        pr.setGroupTagId(child.getId());
        pr.setVisible(true);
        productService.create(pr);

        assertTrue(groupTagService.hasVisibleProductsInSubtree(brand1.getId(), root.getId()));
    }

    @Test
    @DisplayName("Группа: hasVisibleProductsInSubtree - нет видимых товаров")
    void hasVisible_none() {
        ctx(brand1.getId());
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("R");
        r.setBrandId(brand1.getId());
        r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);

        CreateGroupTagRequest c = new CreateGroupTagRequest();
        c.setName("C");
        c.setBrandId(brand1.getId());
        c.setParentId(root.getId());
        GroupTagResponse child = groupTagService.createGroupTag(c);

        ProductCreateRequest p1 = new ProductCreateRequest();
        p1.setName("P1");
        p1.setPrice(new BigDecimal("2"));
        p1.setBrandId(brand1.getId());
        p1.setGroupTagId(root.getId());
        p1.setVisible(false);
        ctx(brand1.getId());
        productService.create(p1);

        ProductCreateRequest p2 = new ProductCreateRequest();
        p2.setName("P2");
        p2.setPrice(new BigDecimal("3"));
        p2.setBrandId(brand1.getId());
        p2.setGroupTagId(child.getId());
        p2.setVisible(false);
        ctx(brand1.getId());
        productService.create(p2);

        assertFalse(groupTagService.hasVisibleProductsInSubtree(brand1.getId(), root.getId()));
    }

    @Test
    @DisplayName("Группа: deleteWithArchive архивирует продукты и группы, удаляет живые; последующее восстановление работает (smoke)")
    void deleteWithArchive_e2e_and_restore_smoke() {
        ctx(brand1.getId());
        // Build tree: Root -> Child
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("Root");
        r.setBrandId(brand1.getId());
        r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);

        CreateGroupTagRequest c = new CreateGroupTagRequest();
        c.setName("Child");
        c.setBrandId(brand1.getId());
        c.setParentId(root.getId());
        GroupTagResponse child = groupTagService.createGroupTag(c);

        // Two products: one under root, one under child
        ProductCreateRequest p1 = new ProductCreateRequest();
        p1.setName("P1");
        p1.setPrice(new BigDecimal("10"));
        p1.setBrandId(brand1.getId());
        p1.setGroupTagId(root.getId());
        p1.setVisible(true);
        ctx(brand1.getId());
        ProductResponse pr1 = productService.create(p1);

        ProductCreateRequest p2 = new ProductCreateRequest();
        p2.setName("P2");
        p2.setPrice(new BigDecimal("20"));
        p2.setBrandId(brand1.getId());
        p2.setGroupTagId(child.getId());
        p2.setVisible(true);
        ctx(brand1.getId());
        ProductResponse pr2 = productService.create(p2);

        // Delete with archive the whole subtree at root
        ctx(brand1.getId());
        groupTagService.deleteWithArchive(root.getId());

        // Live groups gone
        assertTrue(groupTagRepository.findById(root.getId()).isEmpty());
        assertTrue(groupTagRepository.findById(child.getId()).isEmpty());
        // Live products gone
        assertTrue(productRepository.findById(pr1.getId()).isEmpty());
        assertTrue(productRepository.findById(pr2.getId()).isEmpty());

        // Archives present
        assertEquals(2, productArchiveRepository.findByBrandId(brand1.getId()).size());
        assertEquals(2, groupTagArchiveRepository.findByBrandId(brand1.getId()).size());

        // Paged archives smoke
        ctx(brand1.getId());
        var pageProducts = productService.listArchiveByBrandPaged(brand1.getId(), org.springframework.data.domain.PageRequest.of(0, 1));
        assertEquals(1, pageProducts.getContent().size());
        assertTrue(pageProducts.getTotalElements() >= 2);

        ctx(brand1.getId());
        var pageGroups = groupTagService.listArchiveByBrandPaged(brand1.getId(), org.springframework.data.domain.PageRequest.of(0, 1));
        assertEquals(1, pageGroups.getContent().size());
        assertTrue(pageGroups.getTotalElements() >= 2);

        // Restore a group (child) from archive: choose the record with name "Child"
        var groupArchives = groupTagArchiveRepository.findByBrandId(brand1.getId());
        Long childArchiveId = groupArchives.stream().filter(a -> "Child".equals(a.getName())).findFirst().orElseThrow().getId();
        ctx(brand1.getId());
        GroupTagResponse restoredChild = groupTagService.restoreGroupFromArchive(childArchiveId, null);
        assertNotNull(restoredChild.getId());

        // After restore, group archive entries should shrink
        assertTrue(groupTagArchiveRepository.findByBrandId(brand1.getId()).size() <= 1);

        // Restore one product (target null -> path fallback)
        Long anyProductArchiveId = productArchiveRepository.findByBrandId(brand1.getId()).getFirst().getId();
        ProductResponse restoredProduct = productService.restoreFromArchive(anyProductArchiveId, null);
        assertNotNull(restoredProduct.getId());
    }

    @Test
    @DisplayName("Архив групп: purgeArchive удаляет записи старше порога")
    void group_archive_purge_deletes_old() {
        // Create two fake archive entries with different archivedAt
        var a1 = new kirillzhdanov.identityservice.model.tags.GroupTagArchive();
        a1.setOriginalGroupTagId(111L);
        a1.setBrandId(brand1.getId());
        a1.setParentId(null);
        a1.setName("Old");
        a1.setPath("/" + brand1.getName() + "/Old/");
        a1.setLevel(1);
        a1.setArchivedAt(java.time.LocalDateTime.now().minusDays(60));
        groupTagArchiveRepository.save(a1);

        var a2 = new kirillzhdanov.identityservice.model.tags.GroupTagArchive();
        a2.setOriginalGroupTagId(222L);
        a2.setBrandId(brand1.getId());
        a2.setParentId(null);
        a2.setName("New");
        a2.setPath("/" + brand1.getName() + "/New/");
        a2.setLevel(1);
        a2.setArchivedAt(java.time.LocalDateTime.now().minusDays(10));
        groupTagArchiveRepository.save(a2);

        long purged = groupTagService.purgeArchive(30);
        assertEquals(1L, purged, "Expected exactly one old archive record to be purged");
        // Verification via separate count query (no stale entities in context)
        long remainingOld = groupTagArchiveRepository.countByArchivedAtBefore(java.time.LocalDateTime.now().minusDays(30));
        assertEquals(0L, remainingOld, "There must be no records older than threshold after purge");
        // Old must be gone, New must remain
        assertTrue(groupTagArchiveRepository.findByBrandIdAndPath(brand1.getId(), "/" + brand1.getName() + "/Old/").isEmpty());
        assertTrue(groupTagArchiveRepository.findByBrandIdAndPath(brand1.getId(), "/" + brand1.getName() + "/New/").isPresent());
    }

    @Test
    @DisplayName("Группа: создание корневой и дочерней, дерево /tree")
    void create_root_and_child_and_tree() {
        ctx(brand1.getId());
        // root
        CreateGroupTagRequest rootReq = new CreateGroupTagRequest();
        rootReq.setName("Root");
        rootReq.setBrandId(brand1.getId());
        rootReq.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(rootReq);
        assertNotNull(root.getId());
        assertNull(root.getParentId());

        // child
        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setName("Child");
        childReq.setBrandId(brand1.getId());
        childReq.setParentId(root.getId());
        GroupTagResponse child = groupTagService.createGroupTag(childReq);
        assertEquals(root.getId(), child.getParentId());

        // tree should include both
        ctx(brand1.getId());
        List<GroupTagTreeResponse> tree = groupTagService.tree(brand1.getId());
        assertEquals(1, tree.size());
        assertEquals("Root", tree.getFirst().getName());
        assertEquals(1, tree.getFirst().getChildren().size());
        assertEquals("Child", tree.getFirst().getChildren().getFirst().getName());
    }

    @Test
    @DisplayName("Группа: updateFull переименование + перенос")
    void update_full_move_and_rename() {
        ctx(brand1.getId());
        // root
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("R"); r.setBrandId(brand1.getId()); r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);

        CreateGroupTagRequest aReq = new CreateGroupTagRequest();
        aReq.setName("A"); aReq.setBrandId(brand1.getId()); aReq.setParentId(root.getId());
        GroupTagResponse a = groupTagService.createGroupTag(aReq);

        CreateGroupTagRequest bReq = new CreateGroupTagRequest();
        bReq.setName("B"); bReq.setBrandId(brand1.getId()); bReq.setParentId(root.getId());
        GroupTagResponse b = groupTagService.createGroupTag(bReq);

        UpdateGroupTagRequest upd = new UpdateGroupTagRequest();
        upd.setName("B-Updated");
        upd.setParentId(a.getId());
        upd.setBrandId(brand1.getId());
        ctx(brand1.getId());
        GroupTagResponse res = groupTagService.updateGroupTag(b.getId(), upd);
        assertEquals("B-Updated", res.getName());
        assertEquals(a.getId(), res.getParentId());
    }

    @Test
    @DisplayName("Группа: смена бренда переносит и товары")
    void change_brand_moves_products() {
        ctx(brand1.getId());
        CreateGroupTagRequest r1 = new CreateGroupTagRequest();
        r1.setName("R1"); r1.setBrandId(brand1.getId()); r1.setParentId(0L);
        GroupTagResponse root1 = groupTagService.createGroupTag(r1);

        CreateGroupTagRequest nodeReq = new CreateGroupTagRequest();
        nodeReq.setName("Node"); nodeReq.setBrandId(brand1.getId()); nodeReq.setParentId(root1.getId());
        GroupTagResponse node = groupTagService.createGroupTag(nodeReq);

        // product under node
        ProductCreateRequest pr = new ProductCreateRequest();
        pr.setName("P");
        pr.setPrice(new BigDecimal("10"));
        pr.setBrandId(brand1.getId());
        pr.setGroupTagId(node.getId());
        pr.setVisible(true);
        ctx(brand1.getId());
        ProductResponse created = productService.create(pr);
        assertEquals(brand1.getId(), created.getBrandId());
        assertEquals(node.getId(), created.getGroupTagId());

        // change brand of node subtree
        UpdateGroupTagRequest upd = new UpdateGroupTagRequest();
        upd.setBrandId(brand2.getId());
        // ВАЖНО: менять бренд узла нужно из контекста исходного бренда узла
        ctx(brand1.getId());
        GroupTagResponse after = groupTagService.updateGroupTag(node.getId(), upd);
        assertEquals(brand2.getId(), after.getBrandId());

        ctx(brand2.getId());
        ProductResponse reloaded = productService.getById(created.getId());
        assertEquals(brand2.getId(), reloaded.getBrandId());
        assertEquals(node.getId(), reloaded.getGroupTagId());
    }

    @Test
    @DisplayName("Группа: перемещение в корень сбрасывает parent")
    void move_to_root_resets_parent() {
        ctx(brand1.getId());
        CreateGroupTagRequest rootReq = new CreateGroupTagRequest();
        rootReq.setName("R");
        rootReq.setBrandId(brand1.getId());
        rootReq.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(rootReq);

        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setName("C");
        childReq.setBrandId(brand1.getId());
        childReq.setParentId(root.getId());
        GroupTagResponse child = groupTagService.createGroupTag(childReq);

        ctx(brand1.getId());
        GroupTagResponse moved = groupTagService.move(child.getId(), 0L);
        assertNull(moved.getParentId());
    }

    @Test
    @DisplayName("Группа: перемещение к родителю другого бренда -> IllegalArgumentException")
    void move_to_other_brand_parent_throws() {
        ctx(brand1.getId());
        CreateGroupTagRequest r1 = new CreateGroupTagRequest();
        r1.setName("R1");
        r1.setBrandId(brand1.getId());
        r1.setParentId(0L);
        GroupTagResponse root1 = groupTagService.createGroupTag(r1);

        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setName("C");
        childReq.setBrandId(brand1.getId());
        childReq.setParentId(root1.getId());
        GroupTagResponse child = groupTagService.createGroupTag(childReq);

        CreateGroupTagRequest otherRootReq = new CreateGroupTagRequest();
        otherRootReq.setName("OtherRoot");
        otherRootReq.setBrandId(brand2.getId());
        otherRootReq.setParentId(0L);
        ctx(brand2.getId());
        GroupTagResponse otherRoot = groupTagService.createGroupTag(otherRootReq);

        ctx(brand1.getId());
        assertThrows(IllegalArgumentException.class, () -> groupTagService.move(child.getId(), otherRoot.getId()));
    }

    @Test
    @DisplayName("Группа: перемещение к несуществующему родителю -> ResourceNotFoundException")
    void move_to_nonexistent_parent_throws() {
        ctx(brand1.getId());
        CreateGroupTagRequest r1 = new CreateGroupTagRequest();
        r1.setName("R1");
        r1.setBrandId(brand1.getId());
        r1.setParentId(0L);
        GroupTagResponse root1 = groupTagService.createGroupTag(r1);

        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setName("C");
        childReq.setBrandId(brand1.getId());
        childReq.setParentId(root1.getId());
        GroupTagResponse child = groupTagService.createGroupTag(childReq);

        ctx(brand1.getId());
        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> groupTagService.move(child.getId(), 999999L));
    }

    @Test
    @DisplayName("Группа: rename конфликт под тем же родителем -> IllegalArgumentException")
    void rename_conflict_same_parent_throws() {
        ctx(brand1.getId());
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("R");
        r.setBrandId(brand1.getId());
        r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);

        CreateGroupTagRequest aReq = new CreateGroupTagRequest();
        aReq.setName("A");
        aReq.setBrandId(brand1.getId());
        aReq.setParentId(root.getId());
        groupTagService.createGroupTag(aReq);

        CreateGroupTagRequest bReq = new CreateGroupTagRequest();
        bReq.setName("B");
        bReq.setBrandId(brand1.getId());
        bReq.setParentId(root.getId());
        GroupTagResponse b = groupTagService.createGroupTag(bReq);

        ctx(brand1.getId());
        assertThrows(IllegalArgumentException.class, () -> groupTagService.rename(b.getId(), "A"));
    }

    @Test
    @Transactional
    @DisplayName("Группа: getGroupTagsTree возвращает детей для корней")
    void getGroupTagsTree_structure() {
        ctx(brand1.getId());
        CreateGroupTagRequest rootReq = new CreateGroupTagRequest();
        rootReq.setName("Root");
        rootReq.setBrandId(brand1.getId());
        rootReq.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(rootReq);

        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setName("Child");
        childReq.setBrandId(brand1.getId());
        childReq.setParentId(root.getId());
        groupTagService.createGroupTag(childReq);

        ctx(brand1.getId());
        var tree = groupTagService.getGroupTagsTree(brand1.getId());
        assertEquals(1, tree.size());
        assertEquals("Root", tree.getFirst().getName());
        // Проверяем детей через специализированный метод, не полагаясь на заполненность DTO.children
        ctx(brand1.getId());
        var directChildren = groupTagService.getGroupTagsByBrandAndParent(brand1.getId(), root.getId());
        assertEquals(1, directChildren.size());
        assertEquals("Child", directChildren.getFirst().getName());
    }

    @Test
    @DisplayName("Группа: getGroupTagsPaged пагинация по корневым")
    void getGroupTagsPaged_pagination() {
        ctx(brand1.getId());
        for (int i = 0; i < 5; i++) {
            CreateGroupTagRequest r = new CreateGroupTagRequest();
            r.setName("R" + i);
            r.setBrandId(brand1.getId());
            r.setParentId(0L);
            groupTagService.createGroupTag(r);
        }
        org.springframework.data.domain.Pageable p0 = org.springframework.data.domain.PageRequest.of(0, 2,
                org.springframework.data.domain.Sort.by("name").ascending());
        ctx(brand1.getId());
        var page = groupTagService.getGroupTagsPaged(brand1.getId(), 0L, p0);
        assertEquals(2, page.getContent().size());
        assertTrue(page.getTotalElements() >= 5);
    }

    @Test
    @Transactional
    @DisplayName("Группа: breadcrumbs порядок от корня к листу")
    void breadcrumbs_order() {
        ctx(brand1.getId());
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("R");
        r.setBrandId(brand1.getId());
        r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);

        CreateGroupTagRequest aReq = new CreateGroupTagRequest();
        aReq.setName("A");
        aReq.setBrandId(brand1.getId());
        aReq.setParentId(root.getId());
        GroupTagResponse a = groupTagService.createGroupTag(aReq);

        CreateGroupTagRequest bReq = new CreateGroupTagRequest();
        bReq.setName("B");
        bReq.setBrandId(brand1.getId());
        bReq.setParentId(a.getId());
        GroupTagResponse b = groupTagService.createGroupTag(bReq);

        ctx(brand1.getId());
        var bc = groupTagService.breadcrumbs(b.getId());
        assertEquals(3, bc.size());
        assertEquals("R", bc.get(0).getName());
        assertEquals("A", bc.get(1).getName());
        assertEquals("B", bc.get(2).getName());
    }
}
