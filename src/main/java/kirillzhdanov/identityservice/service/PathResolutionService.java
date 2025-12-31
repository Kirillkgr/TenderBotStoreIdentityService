package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.model.tags.GroupTagArchive;
import kirillzhdanov.identityservice.repository.GroupTagArchiveRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PathResolutionService {

    private final GroupTagRepository groupTagRepository;
    private final GroupTagArchiveRepository groupTagArchiveRepository;

    public String normalizePath(String path) {
        if (path == null) return "";
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }

    /**
     * Splits a path like "/Brand/Parent/Child" into segment names and returns
     * all parts after the brand name (i.e., starting from index 1). Empty parts are skipped.
     */
    public java.util.List<String> extractNamesFromPath(String path) {
        String trimmed = normalizePath(path);
        if (trimmed.isBlank()) return java.util.List.of();
        String[] parts = trimmed.split("/");
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 1; i < parts.length; i++) { // skip brand name
            String name = parts[i];
            if (name != null && !name.isBlank()) names.add(name);
        }
        return names;
    }

    public GroupTag createGroupTag(String name, Brand brand, GroupTag parent) {
        GroupTag created = new GroupTag(name, brand, parent);
        return groupTagRepository.save(created);
    }

    /**
     * Resolves the next parent in the chain by:
     * 1) finding existing node by brand/name/parent
     * 2) otherwise trying to restore from archive by exact path prefix
     * 3) otherwise creating a new node by name
     * The provided prefix buffer is updated with the processed segment and trailing slash.
     */
    public GroupTag resolveExistingOrArchivedOrCreate(Brand brand, GroupTag currentParent, String name, StringBuilder prefix) {
        if (name == null || name.isBlank()) return currentParent;
        var existing = groupTagRepository.findByBrandAndNameAndParent(brand, name, currentParent);
        if (existing.isPresent()) {
            GroupTag gt = existing.get();
            prefix.append(name).append("/");
            return gt;
        }
        // try archive by exact path
        prefix.append(name).append("/");
        java.util.Optional<GroupTagArchive> archived = groupTagArchiveRepository.findByBrandIdAndPath(brand.getId(), prefix.toString());
        if (archived.isPresent()) {
            GroupTagArchive ga = archived.get();
            GroupTag created = createGroupTag(ga.getName(), brand, currentParent);
            groupTagArchiveRepository.delete(ga);
            groupTagArchiveRepository.flush();
            return created;
        }
        // create by name
        return createGroupTag(name, brand, currentParent);
    }

    /**
     * Variant without name-creation fallback: returns null if neither existing nor archived node found.
     */
    public GroupTag resolveExistingOrArchived(Brand brand, GroupTag currentParent, String name, StringBuilder prefix) {
        if (name == null || name.isBlank()) return currentParent;
        var existing = groupTagRepository.findByBrandAndNameAndParent(brand, name, currentParent);
        if (existing.isPresent()) {
            GroupTag gt = existing.get();
            prefix.append(name).append("/");
            return gt;
        }
        prefix.append(name).append("/");
        java.util.Optional<GroupTagArchive> archived = groupTagArchiveRepository.findByBrandIdAndPath(brand.getId(), prefix.toString());
        if (archived.isPresent()) {
            GroupTagArchive ga = archived.get();
            GroupTag created = createGroupTag(ga.getName(), brand, currentParent);
            groupTagArchiveRepository.delete(ga);
            groupTagArchiveRepository.flush();
            return created;
        }
        return null;
    }

    /**
     * Internal shared implementation for restoring chain from a path.
     *
     * @param includeLeaf  if true, iterate all segments after brand (parents + leaf); if false, only parents (till leaf-1)
     * @param allowCreate  if true, create a node by name when not found in live/archive; if false, return null on miss
     */
    private GroupTag ensureParents(Brand brand, String path, boolean includeLeaf, boolean allowCreate) {
        if (path == null || path.isBlank()) return null;
        String trimmed = normalizePath(path);
        if (trimmed.isBlank()) return null;
        String[] parts = trimmed.split("/");
        if (parts.length < 2) return null;

        GroupTag currentParent = null;
        StringBuilder prefix = new StringBuilder("/");
        prefix.append(parts[0]).append("/");

        int end = includeLeaf ? parts.length : Math.max(2, parts.length - 1);
        for (int i = 1; i < end; i++) {
            String name = parts[i];
            if (name == null || name.isBlank()) continue;
            if (allowCreate) {
                currentParent = resolveExistingOrArchivedOrCreate(brand, currentParent, name, prefix);
            } else {
                GroupTag resolved = resolveExistingOrArchived(brand, currentParent, name, prefix);
                if (resolved == null) return null;
                currentParent = resolved;
            }
        }
        return currentParent;
    }

    /**
     * Restores parent chain by path using archive-first strategy without creating nodes by name.
     * Iterates all path segments after the brand name (including leaf), returns null if a segment
     * cannot be resolved by existing node or archive.
     */
    public GroupTag ensureParentsArchiveFullNoCreate(Brand brand, String path) {
        return ensureParents(brand, path, true, false);
    }

    /**
     * Restores only parent chain (excluding leaf) using archive-first strategy; creates nodes by name as fallback.
     * Iterates path segments after brand name up to the leaf-1.
     */
    public GroupTag ensureParentsArchiveForParentsOnly(Brand brand, String path) {
        return ensureParents(brand, path, false, true);
    }
}
