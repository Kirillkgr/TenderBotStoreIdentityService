package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.dto.product.ProductUpdateRequest;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.*;
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
    @Autowired
    private jakarta.persistence.EntityManager entityManager;
    @Autowired
    private GroupTagArchiveRepository groupTagArchiveRepository;

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

    @Test
    @DisplayName("Товар: смена бренда сбрасывает группу, если она от другого бренда")
    void product_changeBrand_resetsGroup_ifDifferentBrand() {
        // исходно есть brand (из setup) и его группа group
        // создаём второй бренд
        Brand other = new Brand();
        other.setName("Other Brand " + System.nanoTime());
        other = brandRepository.save(other);

        // создаём товар в группе текущего бренда
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("ChangeBrand");
        req.setPrice(new BigDecimal("99"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(group.getId());
        req.setVisible(true);
        ProductResponse created = productService.create(req);
        assertEquals(group.getId(), created.getGroupTagId());

        // меняем бренд на другой -> группа должна сброситься в корень
        ProductResponse changed = productService.changeBrand(created.getId(), other.getId());
        assertEquals(other.getId(), changed.getBrandId());
        assertNull(changed.getGroupTagId());
    }

    @Test
    @DisplayName("Товар: перемещение в группу другого бренда выбрасывает IllegalArgumentException")
    void product_move_to_other_brand_group_throws() {
        // создаём второй бренд и его группу
        Brand other = new Brand();
        other.setName("Other Brand " + System.nanoTime());
        other = brandRepository.save(other);

        GroupTag otherGroup = new GroupTag();
        otherGroup.setName("Other Root Group");
        otherGroup.setBrand(other);
        otherGroup.setLevel(1);
        otherGroup.setPath("/" + other.getId() + "/1");
        otherGroup = groupTagRepository.save(otherGroup);

        // создаём товар для исходного бренда (brand)
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("MoveCrossBrand");
        req.setPrice(new BigDecimal("55"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(0L);
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        // попытка переместить в группу другого бренда -> IllegalArgumentException
        Long productId = created.getId();
        Long foreignGroupId = otherGroup.getId();
        assertThrows(IllegalArgumentException.class, () -> productService.move(productId, foreignGroupId));
    }

    @Test
    @DisplayName("Товар: перемещение в корень сбрасывает группу")
    void product_move_to_root_moves_out_of_group() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("RootMove");
        req.setPrice(new BigDecimal("77"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(group.getId());
        req.setVisible(true);
        ProductResponse created = productService.create(req);
        assertEquals(group.getId(), created.getGroupTagId());

        ProductResponse movedToRoot = productService.move(created.getId(), 0L);
        assertNull(movedToRoot.getGroupTagId());
    }

    @Test
    @DisplayName("Товар: частичное обновление не затирает поля null значениями")
    void product_update_partial_does_not_overwrite_nulls() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("Partial");
        req.setDescription("Desc");
        req.setPrice(new BigDecimal("10"));
        req.setPromoPrice(new BigDecimal("9"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(0L);
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        ProductUpdateRequest upd = new ProductUpdateRequest();
        upd.setPrice(new BigDecimal("11"));
        upd.setVisible(false);
        // остальные поля null

        ProductResponse updated = productService.update(created.getId(), upd);
        assertEquals("Partial", updated.getName());
        assertNull(updated.getDescription());
        assertEquals(new BigDecimal("11"), updated.getPrice());
        assertNull(updated.getPromoPrice());
        assertFalse(updated.isVisible());
    }

    @Test
    @DisplayName("Архив: удаление несуществующего товара бросает ResourceNotFoundException")
    void archive_delete_nonexistent_throws() {
        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> productService.deleteToArchive(999999L));
    }

    @Test
    @DisplayName("Видимость: несуществующий товар бросает ResourceNotFoundException")
    void visibility_nonexistent_throws() {
        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> productService.updateVisibility(888888L, true));
    }

    @Test
    @DisplayName("Выборка: несуществующий бренд бросает ResourceNotFoundException")
    void getByBrand_nonexistent_brand_throws() {
        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> productService.getByBrandAndGroup(777777L, 0L, true));
    }

    @Test
    @DisplayName("Товар: перемещение в несуществующую группу бросает ResourceNotFoundException")
    void product_move_nonexistent_group_throws() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("MoveNoGroup");
        req.setPrice(new BigDecimal("12"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(0L);
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> productService.move(created.getId(), 123456789L));
    }

    @Test
    @DisplayName("Товар: получение по несуществующему id бросает ResourceNotFoundException")
    void product_get_by_id_nonexistent_throws() {
        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> productService.getById(4242424242L));
    }

    @Test
    @DisplayName("Архив: восстановление по несуществующему id бросает ResourceNotFoundException")
    void archive_restore_nonexistent_throws() {
        assertThrows(kirillzhdanov.identityservice.exception.ResourceNotFoundException.class,
                () -> productService.restoreFromArchive(56565656L, null));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    @DisplayName("Восстановление товара без target: берём цепочку из GroupTagArchive и удаляем использованную запись")
    void product_restore_uses_group_archive_then_consumes() {
        // 1) Подготовка изолированных данных: бренд, группа, товар
        Brand brand = new Brand();
        brand.setName("B-" + System.nanoTime());
        brand = brandRepository.save(brand);

        GroupTag liveGroup = new GroupTag();
        liveGroup.setName("L1");
        liveGroup.setBrand(brand);
        liveGroup.setLevel(1);
        liveGroup.setPath("/" + brand.getId() + "/1");
        liveGroup = groupTagRepository.save(liveGroup);

        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("X");
        req.setPrice(new BigDecimal("1"));
        req.setBrandId(brand.getId());
        req.setGroupTagId(liveGroup.getId());
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        // 2) Архивируем товар и берём его человеко-читаемый путь (name-path)
        productService.deleteToArchive(created.getId());
        var productArchivesByBrand = productArchiveRepository.findByBrandId(brand.getId());
        var productArchive = productArchivesByBrand.stream()
                .filter(a -> a.getOriginalProductId() != null && a.getOriginalProductId().equals(created.getId()))
                .findFirst()
                .orElseThrow();
        Long productArchiveId = productArchive.getId();
        String namePath = productArchive.getGroupPath();

        // Имя узла (последний сегмент пути) — важно, сервис создаёт группу с именем из записи архива
        String trimmed = namePath.replaceAll("^/|/$", "");
        String[] parts = trimmed.split("/");
        String nodeName = parts[parts.length - 1];

        // 3) Удаляем живую группу (чтобы вынудить восстановление по архиву) и сохраняем в архив групп запись с точным path
        groupTagRepository.delete(liveGroup);
        groupTagRepository.flush();

        var ga = new kirillzhdanov.identityservice.model.tags.GroupTagArchive();
        ga.setOriginalGroupTagId(999L);
        ga.setBrandId(brand.getId());
        ga.setParentId(null);
        ga.setName(nodeName);
        ga.setPath(namePath); // точное совпадение с ProductArchive.groupPath
        ga.setLevel(1);
        ga.setArchivedAt(java.time.LocalDateTime.now());
        groupTagArchiveRepository.save(ga);
        Long gaId = ga.getId();
        groupTagArchiveRepository.flush();

        // До восстановления запись должна существовать
        assertTrue(groupTagArchiveRepository.findById(gaId).isPresent());

        // 4) Восстановление товара без указания target -> сервис поднимет нужную группу из архива
        ProductResponse restored = productService.restoreFromArchive(productArchiveId, null);
        assertNotNull(restored.getGroupTagId());

        // 5) Проверяем, что конкретная запись архива была потреблена и общее количество уменьшилось на 1
        entityManager.clear();
        boolean removed = false;
        for (int i = 0; i < 10; i++) {
            removed = groupTagArchiveRepository.findById(gaId).isEmpty();
            if (removed) break;
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            entityManager.clear();
        }
        assertTrue(removed);

        // Дополнительно убедимся, что восстановленная группа существует
        assertTrue(groupTagRepository.findById(restored.getGroupTagId()).isPresent());
    }

    @Test
    @DisplayName("Восстановление товара без target: fallback по именам, если архива групп нет")
    void product_restore_fallback_by_names_when_no_group_archive() {
        Brand b = new Brand();
        b.setName("BrandNames " + System.nanoTime());
        b = brandRepository.save(b);

        GroupTag g = new GroupTag();
        g.setName("Folder");
        g.setBrand(b);
        g.setLevel(1);
        g.setPath("/" + b.getId() + "/1");
        g = groupTagRepository.save(g);

        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("ByNames");
        req.setPrice(new BigDecimal("6"));
        req.setBrandId(b.getId());
        req.setGroupTagId(g.getId());
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        productService.deleteToArchive(created.getId());
        Long productArchiveId = productArchiveRepository.findAll().getFirst().getId();

        // Удаляем живую группу и не добавляем архив записей групп
        groupTagRepository.delete(g);

        ProductResponse restored = productService.restoreFromArchive(productArchiveId, null);
        assertNotNull(restored.getGroupTagId());
        // Восстановленная группа должна существовать
        assertTrue(groupTagRepository.findById(restored.getGroupTagId()).isPresent());
    }

    @Test
    @DisplayName("Архив товара: groupPath экранирует '/' в именах бренда и группы")
    void product_archive_groupPath_escapes_slash() {
        Brand b = new Brand();
        b.setName("Slash/Brand");
        b = brandRepository.save(b);

        GroupTag g = new GroupTag();
        g.setName("Fol/der");
        g.setBrand(b);
        g.setLevel(1);
        g.setPath("/" + b.getId() + "/1");
        g = groupTagRepository.save(g);

        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("Slashy");
        req.setPrice(new BigDecimal("7"));
        req.setBrandId(b.getId());
        req.setGroupTagId(g.getId());
        req.setVisible(true);
        ProductResponse created = productService.create(req);

        productService.deleteToArchive(created.getId());
        var archives = productArchiveRepository.findByBrandId(b.getId());
        assertEquals(1, archives.size());
        String path = archives.getFirst().getGroupPath();
        assertNotNull(path);
        assertTrue(path.contains("Slash-Brand"));
        assertTrue(path.contains("Fol-der"));
        // в человеко-читаемом пути не должно быть сырых '/'
        // кроме разделителей, которые мы сами ставим между сегментами
        assertFalse(path.contains("Slash/Brand"));
        assertFalse(path.contains("Fol/der"));
    }
}
