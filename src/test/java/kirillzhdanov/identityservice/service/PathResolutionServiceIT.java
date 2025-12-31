package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PathResolutionServiceIT extends IntegrationTestBase {

    @Autowired
    private PathResolutionService pathResolutionService;
    @Autowired
    private GroupTagRepository groupTagRepository;
    @Autowired
    private BrandRepository brandRepository;

    private Brand brand;

    @BeforeEach
    void initBrand() {
        Optional<Brand> any = brandRepository.findAll().stream().findFirst();
        assertTrue(any.isPresent(), "Brand should be present from IntegrationTestBase setup");
        brand = any.get();
    }

    @Test
    void ensureParentsArchiveFullNoCreate_traversesExistingChainIncludingLeaf() {
        String suf = UUID.randomUUID().toString().substring(0, 8);
        String A = "A_" + suf;
        String B = "B_" + suf;
        String C = "C_" + suf;
        GroupTag a = groupTagRepository.save(new GroupTag(A, brand, null));
        GroupTag b = groupTagRepository.save(new GroupTag(B, brand, a));
        GroupTag c = groupTagRepository.save(new GroupTag(C, brand, b));

        GroupTag resolved = pathResolutionService.ensureParentsArchiveFullNoCreate(brand, "/" + brand.getName() + "/" + A + "/" + B + "/" + C + "/");
        assertNotNull(resolved);
        assertEquals(C, resolved.getName());
    }

    @Test
    void ensureParentsArchiveForParentsOnly_createsMissingParents_andReturnsLastParent() {
        String suf = UUID.randomUUID().toString().substring(0, 8);
        String A = "A_" + suf;
        String B = "B_" + suf;
        String C = "C_" + suf;
        GroupTag a = groupTagRepository.save(new GroupTag(A, brand, null));
        GroupTag lastParent = pathResolutionService.ensureParentsArchiveForParentsOnly(brand, "/" + brand.getName() + "/" + A + "/" + B + "/" + C + "/");
        assertNotNull(lastParent);
        assertEquals(B, lastParent.getName());
        // Ensure B was created with parent A
        GroupTag bLive = groupTagRepository.findByBrandAndNameAndParent(brand, B, a).orElse(null);
        assertNotNull(bLive);
    }
}
