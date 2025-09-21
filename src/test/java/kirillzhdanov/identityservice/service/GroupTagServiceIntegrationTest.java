package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.dto.group.GroupTagTreeResponse;
import kirillzhdanov.identityservice.dto.group.UpdateGroupTagRequest;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import kirillzhdanov.identityservice.dto.product.ProductResponse;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
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
public class GroupTagServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private GroupTagService groupTagService;
    @Autowired
    private ProductService productService;
    @Autowired
    private BrandRepository brandRepository;

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

    @Test
    @DisplayName("Группа: создание корневой и дочерней, дерево /tree")
    void create_root_and_child_and_tree() {
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
        List<GroupTagTreeResponse> tree = groupTagService.tree(brand1.getId());
        assertEquals(1, tree.size());
        assertEquals("Root", tree.getFirst().getName());
        assertEquals(1, tree.getFirst().getChildren().size());
        assertEquals("Child", tree.getFirst().getChildren().getFirst().getName());
    }

    @Test
    @DisplayName("Группа: updateFull переименование + перенос")
    void update_full_move_and_rename() {
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
        GroupTagResponse res = groupTagService.updateGroupTag(b.getId(), upd);
        assertEquals("B-Updated", res.getName());
        assertEquals(a.getId(), res.getParentId());
    }

    @Test
    @DisplayName("Группа: смена бренда переносит и товары")
    void change_brand_moves_products() {
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
        ProductResponse created = productService.create(pr);
        assertEquals(brand1.getId(), created.getBrandId());
        assertEquals(node.getId(), created.getGroupTagId());

        // change brand of node subtree
        UpdateGroupTagRequest upd = new UpdateGroupTagRequest();
        upd.setBrandId(brand2.getId());
        GroupTagResponse after = groupTagService.updateGroupTag(node.getId(), upd);
        assertEquals(brand2.getId(), after.getBrandId());

        ProductResponse reloaded = productService.getById(created.getId());
        assertEquals(brand2.getId(), reloaded.getBrandId());
        assertEquals(node.getId(), reloaded.getGroupTagId());
    }
}
