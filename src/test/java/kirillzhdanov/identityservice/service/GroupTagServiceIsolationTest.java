package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.dto.group.GroupTagTreeResponse;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GroupTagServiceIsolationTest extends IntegrationTestBase {

    @Autowired
    private GroupTagService groupTagService;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    private Brand b1;
    private Brand b2;

    @BeforeEach
    void seed() {
        MasterAccount m1 = new MasterAccount();
        m1.setName("M1");
        MasterAccount m2 = new MasterAccount();
        m2.setName("M2");
        m1 = masterAccountRepository.save(m1);
        m2 = masterAccountRepository.save(m2);
        String uniq = String.valueOf(System.nanoTime());
        b1 = Brand.builder().name("B1-" + uniq).build();
        b1.setMaster(m1);
        b2 = Brand.builder().name("B2-" + uniq).build();
        b2.setMaster(m2);
        brandRepository.saveAll(java.util.List.of(b1, b2));

        // Build simple tree under b1: Root -> Child
        CreateGroupTagRequest r = new CreateGroupTagRequest();
        r.setName("Root");
        r.setBrandId(b1.getId());
        r.setParentId(0L);
        GroupTagResponse root = groupTagService.createGroupTag(r);
        CreateGroupTagRequest c = new CreateGroupTagRequest();
        c.setName("Child");
        c.setBrandId(b1.getId());
        c.setParentId(root.getId());
        groupTagService.createGroupTag(c);

        // Another root under b2
        CreateGroupTagRequest r2 = new CreateGroupTagRequest();
        r2.setName("OtherRoot");
        r2.setBrandId(b2.getId());
        r2.setParentId(0L);
        groupTagService.createGroupTag(r2);
    }

    @Test
    @DisplayName("tree(brandId): возвращает только группы выбранного бренда")
    void tree_isolated_by_brand() {
        List<GroupTagTreeResponse> t1 = groupTagService.tree(b1.getId());
        assertEquals(1, t1.size());
        assertEquals("Root", t1.getFirst().getName());

        List<GroupTagTreeResponse> t2 = groupTagService.tree(b2.getId());
        assertEquals(1, t2.size());
        assertEquals("OtherRoot", t2.getFirst().getName());
    }

    @Test
    @DisplayName("getGroupTagsByBrandAndParent: изоляция по бренду")
    void get_by_brand_and_parent_isolated() {
        var rootsB1 = groupTagService.getGroupTagsByBrandAndParent(b1.getId(), 0L);
        assertTrue(rootsB1.stream().allMatch(gt -> gt.getBrandId().equals(b1.getId())));

        var rootsB2 = groupTagService.getGroupTagsByBrandAndParent(b2.getId(), 0L);
        assertTrue(rootsB2.stream().allMatch(gt -> gt.getBrandId().equals(b2.getId())));
    }

    @Test
    @DisplayName("move: запрет перемещения к родителю другого бренда")
    void move_cross_brand_forbidden() {
        // Make two roots in different brands
        var rootB1 = groupTagService.getGroupTagsByBrandAndParent(b1.getId(), 0L).getFirst();
        var rootB2 = groupTagService.getGroupTagsByBrandAndParent(b2.getId(), 0L).getFirst();

        // Create child under b1
        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setName("X");
        childReq.setBrandId(b1.getId());
        childReq.setParentId(rootB1.getId());
        GroupTagResponse child = groupTagService.createGroupTag(childReq);

        assertThrows(IllegalArgumentException.class, () -> groupTagService.move(child.getId(), rootB2.getId()));
    }
}
