package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.model.tags.GroupTagArchive;
import kirillzhdanov.identityservice.repository.GroupTagArchiveRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PathResolutionServiceTest {

    private GroupTagRepository groupTagRepository;
    private GroupTagArchiveRepository groupTagArchiveRepository;
    private PathResolutionService service;

    private Brand brand;

    @BeforeEach
    void setUp() {
        groupTagRepository = mock(GroupTagRepository.class);
        groupTagArchiveRepository = mock(GroupTagArchiveRepository.class);
        service = new PathResolutionService(groupTagRepository, groupTagArchiveRepository);

        // Brand with id for archive lookups
        brand = mock(Brand.class);
        when(brand.getId()).thenReturn(1L);
    }

    @Test
    void ensureParentsArchiveFullNoCreate_success_whenAllSegmentsExist() {
        String path = "/Brand/A/B/C/";

        GroupTag a = new GroupTag("A", brand, null);
        GroupTag b = new GroupTag("B", brand, a);
        GroupTag c = new GroupTag("C", brand, b);

        when(groupTagRepository.findByBrandAndNameAndParent(eq(brand), eq("A"), isNull())).thenReturn(Optional.of(a));
        when(groupTagRepository.findByBrandAndNameAndParent(eq(brand), eq("B"), eq(a))).thenReturn(Optional.of(b));
        when(groupTagRepository.findByBrandAndNameAndParent(eq(brand), eq("C"), eq(b))).thenReturn(Optional.of(c));

        GroupTag result = service.ensureParentsArchiveFullNoCreate(brand, path);
        assertNotNull(result);
        assertEquals("C", result.getName());
    }

    @Test
    void ensureParentsArchiveFullNoCreate_returnsNull_whenMissingAndNotArchived() {
        String path = "/Brand/A/B/C/";

        GroupTag a = new GroupTag("A", brand, null);
        when(groupTagRepository.findByBrandAndNameAndParent(eq(brand), eq("A"), isNull())).thenReturn(Optional.of(a));
        // B not found
        when(groupTagRepository.findByBrandAndNameAndParent(eq(brand), eq("B"), eq(a))).thenReturn(Optional.empty());
        // No archive hit for B
        when(groupTagArchiveRepository.findByBrandIdAndPath(anyLong(), anyString())).thenReturn(Optional.empty());

        GroupTag result = service.ensureParentsArchiveFullNoCreate(brand, path);
        assertNull(result);
    }

    @Test
    void ensureParentsArchiveForParentsOnly_usesArchive_thenCreates_andReturnsParent() {
        String path = "/Brand/A/B/C/"; // parents are A and B; leaf is C

        GroupTag a = new GroupTag("A", brand, null);
        when(groupTagRepository.findByBrandAndNameAndParent(eq(brand), eq("A"), isNull())).thenReturn(Optional.of(a));

        // B not found live, but present in archive by exact path
        GroupTagArchive archivedB = mock(GroupTagArchive.class);
        when(archivedB.getName()).thenReturn("B");
        when(groupTagArchiveRepository.findByBrandIdAndPath(eq(1L), eq("/Brand/A/B/"))).thenReturn(Optional.of(archivedB));

        // When creating B from archive
        when(groupTagRepository.save(any(GroupTag.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupTag resultParent = service.ensureParentsArchiveForParentsOnly(brand, path);
        assertNotNull(resultParent);
        assertEquals("B", resultParent.getName());
        // Archive entry should be deleted after restore
        verify(groupTagArchiveRepository).delete(eq(archivedB));
        verify(groupTagArchiveRepository).flush();
    }
}
