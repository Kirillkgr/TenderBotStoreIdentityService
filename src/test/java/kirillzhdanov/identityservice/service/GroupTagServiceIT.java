package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.dto.group.UpdateGroupTagRequest;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import kirillzhdanov.identityservice.repository.GroupTagArchiveRepository;
import kirillzhdanov.identityservice.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroupTagServiceIT extends IntegrationTestBase {

    @Autowired
    private GroupTagService groupTagService;
    @Autowired
    private GroupTagRepository groupTagRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private GroupTagArchiveRepository groupTagArchiveRepository;

    private Brand brand;

    @BeforeEach
    void initBrand() {
        brand = brandRepository.findAll().stream().findFirst().orElseThrow();
        // Satisfy ContextGuards.requireBrandInContextOr404 checks inside GroupTagService
        TenantContext.setBrandId(brand.getId());
    }

    @Test
    void create_move_rename_flow() {
        String suf = UUID.randomUUID().toString().substring(0, 8);
        // Create root
        CreateGroupTagRequest rootReq = new CreateGroupTagRequest();
        rootReq.setBrandId(brand.getId());
        rootReq.setName("Root_" + suf);
        GroupTagResponse root = groupTagService.createGroupTag(rootReq);
        assertNotNull(root.getId());

        // Create child under root
        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setBrandId(brand.getId());
        childReq.setParentId(root.getId());
        childReq.setName("Child_" + suf);
        GroupTagResponse child = groupTagService.createGroupTag(childReq);
        assertNotNull(child.getId());

        // Create another parent and move child to it
        CreateGroupTagRequest newParentReq = new CreateGroupTagRequest();
        newParentReq.setBrandId(brand.getId());
        newParentReq.setName("NewParent_" + suf);
        GroupTagResponse newParent = groupTagService.createGroupTag(newParentReq);

        GroupTagResponse moved = groupTagService.move(child.getId(), newParent.getId());
        assertEquals(newParent.getId(), moved.getParentId());

        // Rename moved child
        UpdateGroupTagRequest upd = new UpdateGroupTagRequest();
        upd.setBrandId(brand.getId());
        upd.setName("RenamedChild_" + suf);
        GroupTagResponse renamed = groupTagService.updateGroupTag(child.getId(), upd);
        assertEquals("RenamedChild_" + suf, renamed.getName());

        // Verify in repository
        GroupTag entity = groupTagRepository.findById(renamed.getId()).orElseThrow();
        assertEquals("RenamedChild_" + suf, entity.getName());
        assertEquals(newParent.getId(), entity.getParent().getId());
    }

    @Test
    void restoreGroupFromArchive_end_to_end() {
        String suf = UUID.randomUUID().toString().substring(0, 8);
        // Create parent and child
        CreateGroupTagRequest parentReq = new CreateGroupTagRequest();
        parentReq.setBrandId(brand.getId());
        parentReq.setName("Par_" + suf);
        GroupTagResponse parent = groupTagService.createGroupTag(parentReq);

        CreateGroupTagRequest childReq = new CreateGroupTagRequest();
        childReq.setBrandId(brand.getId());
        childReq.setParentId(parent.getId());
        childReq.setName("Ch_" + suf);
        GroupTagResponse child = groupTagService.createGroupTag(childReq);

        // Archive child
        groupTagService.deleteWithArchive(child.getId());

        // Find archive entry for child
        var archives = groupTagService.listArchiveByBrand(brand.getId());
        var childArch = archives.stream().filter(a -> a.getName().equals("Ch_" + suf)).findFirst().orElseThrow();

        // Restore with explicit target parent id
        GroupTagResponse restored = groupTagService.restoreGroupFromArchive(childArch.getId(), parent.getId());
        assertEquals("Ch_" + suf, restored.getName());
        assertEquals(parent.getId(), restored.getParentId());
        // Archive record removed
        assertTrue(groupTagService.listArchiveByBrand(brand.getId()).stream().noneMatch(a -> a.getId().equals(childArch.getId())));
    }

    @Test
    void purgeArchive_removesOldEntries() {
        String suf = UUID.randomUUID().toString().substring(0, 8);
        // Create and archive two groups
        for (int i = 0; i < 2; i++) {
            CreateGroupTagRequest req = new CreateGroupTagRequest();
            req.setBrandId(brand.getId());
            req.setName("OldArch_" + suf + "_" + i);
            GroupTagResponse g = groupTagService.createGroupTag(req);
            groupTagService.deleteWithArchive(g.getId());
        }

        // Move archivedAt to the past for all current archive records of this brand
        var allArchEntities = groupTagArchiveRepository.findByBrandId(brand.getId());
        for (var ent : allArchEntities) {
            ent.setArchivedAt(LocalDateTime.now().minusDays(120));
            groupTagArchiveRepository.save(ent);
        }

        long deleted = groupTagService.purgeArchive(90);
        assertTrue(deleted >= 2);
        // Verify none remain older than threshold
        long remaining = groupTagArchiveRepository.countByArchivedAtBefore(LocalDateTime.now().minusDays(90));
        assertEquals(0, remaining);
    }
}
