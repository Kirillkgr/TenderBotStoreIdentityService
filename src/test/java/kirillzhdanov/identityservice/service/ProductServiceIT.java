package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.dto.product.ProductUpdateRequest;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import kirillzhdanov.identityservice.repository.ProductArchiveRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import kirillzhdanov.identityservice.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProductServiceIT extends IntegrationTestBase {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductArchiveRepository productArchiveRepository;
    @Autowired
    private GroupTagRepository groupTagRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private GroupTagService groupTagService;

    private Brand brand;

    @BeforeEach
    void initBrand() {
        brand = brandRepository.findAll().stream().findFirst().orElseThrow();
        TenantContext.setBrandId(brand.getId());
    }

    @Test
    void restoreFromArchive_withoutTarget_usesPathChain_andRecreatesParents() {
        String suf = UUID.randomUUID().toString().substring(0, 8);

        // Prepare group chain A/B
        GroupTag a = groupTagRepository.save(new GroupTag("A_" + suf, brand, null));
        GroupTag b = groupTagRepository.save(new GroupTag("B_" + suf, brand, a));

        // Create product under B and archive it
        ProductCreateRequest create = new ProductCreateRequest();
        create.setBrandId(brand.getId());
        create.setName("P_" + suf);
        create.setPrice(new BigDecimal("1.00"));
        create.setVisible(true);
        ProductResponse pr = productService.create(create);
        // Move into B
        productService.move(pr.getId(), b.getId());
        // Archive
        productService.deleteToArchive(pr.getId());
        var archives = productArchiveRepository.findAll();
        assertFalse(archives.isEmpty());
        Long archiveId = archives.getFirst().getId();

        // Remove groups by archiving them so live lookup fails
        groupTagService.deleteWithArchive(a.getId());

        // Now restore product by archive without target groupId (null) — should use groupPath chain
        ProductResponse restored = productService.restoreFromArchive(archiveId, null);
        assertNotNull(restored.getId());
        // Verify group B was recreated and product linked
        GroupTag recreatedB = groupTagRepository.findByBrandAndNameAndParent(brand, "B_" + suf,
                groupTagRepository.findByBrandAndNameAndParent(brand, "A_" + suf, null).orElse(null)).orElse(null);
        assertNotNull(recreatedB);
        assertEquals(recreatedB.getId(), restored.getGroupTagId());
    }

    @Test
    void productArchive_list_and_pagination() {
        String suf = UUID.randomUUID().toString().substring(0, 8);

        // Create and archive 3 products
        for (int i = 0; i < 3; i++) {
            ProductCreateRequest req = new ProductCreateRequest();
            req.setBrandId(brand.getId());
            req.setName("PA_" + suf + "_" + i);
            req.setPrice(new BigDecimal("1.00"));
            req.setVisible(true);
            ProductResponse r = productService.create(req);
            productService.deleteToArchive(r.getId());
        }

        // List all
        var all = productService.listArchiveByBrand(brand.getId());
        assertTrue(all.size() >= 3);

        // Paged
        Page<kirillzhdanov.identityservice.dto.product.ProductArchiveResponse> page1 = productService.listArchiveByBrandPaged(brand.getId(), PageRequest.of(0, 2));
        assertEquals(2, page1.getSize());
        assertTrue(page1.getTotalElements() >= 3);
    }

    @Test
    void create_update_move_visibility_and_archive_restore() {
        String suf = UUID.randomUUID().toString().substring(0, 8);

        // Create product
        ProductCreateRequest create = new ProductCreateRequest();
        create.setBrandId(brand.getId());
        create.setName("Prod_" + suf);
        create.setDescription("Desc_" + suf);
        create.setPrice(new BigDecimal("10.50"));
        create.setPromoPrice(new BigDecimal("9.99"));
        create.setVisible(true);
        ProductResponse created = productService.create(create);
        assertNotNull(created.getId());
        Long pid = created.getId();

        // Update
        ProductUpdateRequest upd = new ProductUpdateRequest();
        upd.setName("ProdU_" + suf);
        upd.setDescription("DescU_" + suf);
        upd.setPrice(new BigDecimal("12.00"));
        upd.setPromoPrice(new BigDecimal("10.00"));
        upd.setVisible(Boolean.FALSE);
        ProductResponse updated = productService.update(pid, upd);
        assertEquals("ProdU_" + suf, updated.getName());
        assertFalse(updated.isVisible());

        // Create group and move product into it
        GroupTag parent = groupTagRepository.save(new GroupTag("G_" + suf, brand, null));
        ProductResponse moved = productService.move(pid, parent.getId());
        assertEquals(parent.getId(), moved.getGroupTagId());

        // Toggle visibility on
        ProductResponse visOn = productService.updateVisibility(pid, true);
        assertTrue(visOn.isVisible());

        // Archive and restore
        productService.deleteToArchive(pid);
        assertTrue(productRepository.findById(pid).isEmpty());
        var archives = productArchiveRepository.findAll();
        assertFalse(archives.isEmpty());
        Long archiveId = archives.getFirst().getId();
        ProductResponse restored = productService.restoreFromArchive(archiveId, parent.getId());
        assertNotNull(restored.getId());
        assertEquals(parent.getId(), restored.getGroupTagId());
        // After restore, archive entry removed
        assertTrue(productArchiveRepository.findById(archiveId).isEmpty());
    }
}
