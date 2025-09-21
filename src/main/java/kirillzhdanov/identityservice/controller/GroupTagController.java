package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.dto.group.UpdateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagTreeResponse;
import kirillzhdanov.identityservice.service.GroupTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/auth/v1/group-tags")
@RequiredArgsConstructor
public class GroupTagController {

    private final GroupTagService groupTagService;

    @PostMapping
    public ResponseEntity<GroupTagResponse> createGroupTag(
            @Valid @RequestBody CreateGroupTagRequest request) {
        GroupTagResponse response = groupTagService.createGroupTag(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/by-brand/{brandId}")
    public ResponseEntity<List<GroupTagResponse>> getGroupTagsByBrandAndParent(
            @PathVariable Long brandId,
            @RequestParam(required = false) Long parentId) {
        
        List<GroupTagResponse> response = groupTagService.getGroupTagsByBrandAndParent(brandId, parentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tree/{brandId}")
    public ResponseEntity<List<GroupTagResponse>> getGroupTagsTree(@PathVariable Long brandId) {
        List<GroupTagResponse> response = groupTagService.getGroupTagsTree(brandId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tree/{brandId}/full")
    public ResponseEntity<List<GroupTagTreeResponse>> tree(@PathVariable Long brandId) {
        return ResponseEntity.ok(groupTagService.tree(brandId));
    }

    @GetMapping("/by-brand/{brandId}/paged")
    public ResponseEntity<Page<GroupTagResponse>> getGroupTagsPaged(
            @PathVariable Long brandId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction dir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortParts[0]));
        return ResponseEntity.ok(groupTagService.getGroupTagsPaged(brandId, parentId, pageable));
    }

    @PutMapping("/{groupTagId}")
    public ResponseEntity<GroupTagResponse> rename(
            @PathVariable Long groupTagId,
            @RequestParam String name
    ) {
        return ResponseEntity.ok(groupTagService.rename(groupTagId, name));
    }

    // Единый апдейт: смена бренда, перенос, переименование — одним запросом
    @PutMapping("/{groupTagId}/full")
    public ResponseEntity<GroupTagResponse> updateFull(
            @PathVariable Long groupTagId,
            @Valid @RequestBody UpdateGroupTagRequest request
    ) {
        return ResponseEntity.ok(groupTagService.updateGroupTag(groupTagId, request));
    }

    @PatchMapping("/{groupTagId}/move")
    public ResponseEntity<GroupTagResponse> move(
            @PathVariable Long groupTagId,
            @RequestParam(required = false) Long parentId
    ) {
        return ResponseEntity.ok(groupTagService.move(groupTagId, parentId));
    }

    @PatchMapping("/{groupTagId}/brand")
    public ResponseEntity<GroupTagResponse> changeBrand(
            @PathVariable Long groupTagId,
            @RequestParam Long brandId
    ) {
        return ResponseEntity.ok(groupTagService.changeBrand(groupTagId, brandId));
    }

    @GetMapping("/breadcrumbs/{groupTagId}")
    public ResponseEntity<List<GroupTagResponse>> breadcrumbs(@PathVariable Long groupTagId) {
        return ResponseEntity.ok(groupTagService.breadcrumbs(groupTagId));
    }

    @DeleteMapping("/{groupTagId}")
    public ResponseEntity<Void> deleteWithArchive(@PathVariable Long groupTagId) {
        groupTagService.deleteWithArchive(groupTagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/archive/purge")
    public ResponseEntity<Long> purgeArchive(@RequestParam(defaultValue = "90") int olderThanDays) {
        long deleted = groupTagService.purgeArchive(olderThanDays);
        return ResponseEntity.ok(deleted);
    }
}
