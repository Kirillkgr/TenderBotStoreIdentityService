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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ProductServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductArchiveRepository productArchiveRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private GroupTagRepository groupTagRepository;

    private Brand brand;
    private GroupTag group;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    @BeforeEach
    void setup(TestInfo testInfo) {
        int n = COUNTER.incrementAndGet();
        System.out.printf("[%02d] %s%n", n, testInfo.getDisplayName());

        brand = new Brand();
        brand.setName("Test Brand " + System.nanoTime());
        brand = brandRepository.save(brand);

        group = new GroupTag();
        group.setName("Root Group");
        group.setBrand(brand);
        group.setLevel(1);
        group.setPath("/" + brand.getId() + "/1");
        group = groupTagRepository.save(group);
    }

    @Test
    @DisplayName("Товар: создание в корне и обновление полей")
    void product_create_and_update() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("Water");
        req.setDescription("0.5L");
        req.setPrice(new BigDecimal("50"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(0L);
        req.setVisible(true);

        ProductResponse created = productService.create(req);
        assertNotNull(created.getId());
        assertNull(created.getGroupTagId());

        ProductUpdateRequest upd = new ProductUpdateRequest();
        upd.setName("Water Updated");
        upd.setDescription("0.5L still");
        upd.setPrice(new BigDecimal("45"));
        upd.setPromoPrice(new BigDecimal("40"));
        upd.setVisible(true);

        ProductResponse updated = productService.update(created.getId(), upd);
        assertEquals("Water Updated", updated.getName());
        assertEquals(new BigDecimal("45"), updated.getPrice());
        assertEquals(new BigDecimal("40"), updated.getPromoPrice());
    }

    @Test
    @DisplayName("Товар: перемещение в группу и переключение видимости")
    void product_move_and_visibility() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("X");
        req.setPrice(new BigDecimal("10"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(0L);
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        ProductResponse moved = productService.move(created.getId(), group.getId());
        assertEquals(group.getId(), moved.getGroupTagId());

        ProductResponse invisible = productService.updateVisibility(created.getId(), false);
        assertFalse(invisible.isVisible());
    }

    @Test
    @DisplayName("Товар: удаление в архив и список архива по бренду")
    void product_delete_to_archive_and_list() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("Y");
        req.setPrice(new BigDecimal("20"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(group.getId());
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        productService.deleteToArchive(created.getId());
        assertTrue(productRepository.findById(created.getId()).isEmpty());
        assertEquals(1, productService.listArchiveByBrand(brand.getId()).size());
    }

    @Test
    @DisplayName("Товар: восстановление из архива и очистка архива")
    void product_restore_and_purge_archive() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("Z");
        req.setPrice(new BigDecimal("30"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(0L);
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        productService.deleteToArchive(created.getId());
        Long archiveId = productArchiveRepository.findAll().getFirst().getId();
        ProductResponse restored = productService.restoreFromArchive(archiveId, group.getId());
        assertNotNull(restored.getId());
        assertEquals(group.getId(), restored.getGroupTagId());
        assertEquals(0, productArchiveRepository.count());

        long purged = productService.purgeArchive(90);
        assertEquals(0, purged);
    }

    @Test
    @DisplayName("Товар: получение по id")
    void product_get_by_id() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("GetMe");
        req.setPrice(new BigDecimal("15"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(0L);
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        ProductResponse loaded = productService.getById(created.getId());
        assertEquals("GetMe", loaded.getName());
    }

    @Test
    @DisplayName("Товар: фильтрация по visibleOnly для бренда/группы")
    void getByBrandAndGroup_filtersVisible() {
        // two products: one visible, one hidden
        ProductCreateRequest req1 = new ProductCreateRequest();
        req1.setName("A");
        req1.setPrice(new BigDecimal("10"));
        req1.setBrandId(brand.getId());
        req1.setGroupTagId(group.getId());
        req1.setVisible(true);
        ProductResponse p1 = productService.create(req1);

        ProductCreateRequest req2 = new ProductCreateRequest();
        req2.setName("B");
        req2.setPrice(new BigDecimal("20"));
        req2.setBrandId(brand.getId());
        req2.setGroupTagId(group.getId());
        req2.setVisible(false);
        productService.create(req2);

        List<ProductResponse> onlyVisible = productService.getByBrandAndGroup(brand.getId(), group.getId(), true);
        assertEquals(1, onlyVisible.size());
        assertEquals(p1.getId(), onlyVisible.getFirst().getId());

        List<ProductResponse> all = productService.getByBrandAndGroup(brand.getId(), group.getId(), false);
        assertEquals(2, all.size());
    }
}
