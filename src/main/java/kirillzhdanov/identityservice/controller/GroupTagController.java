package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.group.*;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.GroupTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/v1/group-tags")
@RequiredArgsConstructor
public class GroupTagController {

    private final GroupTagService groupTagService;
    private final RbacGuard rbacGuard;

    @PostMapping
    @Operation(summary = "Создать группу тегов", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<GroupTagResponse> createGroupTag(
            @Valid @RequestBody CreateGroupTagRequest request) {
        rbacGuard.requireOwnerOrAdmin();
        GroupTagResponse response = groupTagService.createGroupTag(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/by-brand/{brandId}")
    @Operation(summary = "Группы тегов по бренду/родителю", description = "Требования: аутентификация. Возвращает только доступные в текущем контексте.")
    public ResponseEntity<List<GroupTagResponse>> getGroupTagsByBrandAndParent(
            @PathVariable Long brandId,
            @RequestParam(required = false) Long parentId) {
        
        List<GroupTagResponse> response = groupTagService.getGroupTagsByBrandAndParent(brandId, parentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tree/{brandId}")
    @Operation(summary = "Дерево групп тегов", description = "Требования: аутентификация. Данные в пределах контекста.")
    public ResponseEntity<List<GroupTagResponse>> getGroupTagsTree(@PathVariable Long brandId) {
        List<GroupTagResponse> response = groupTagService.getGroupTagsTree(brandId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tree/{brandId}/full")
    @Operation(summary = "Полное дерево групп тегов", description = "Требования: аутентификация. Данные в пределах контекста.")
    public ResponseEntity<List<GroupTagTreeResponse>> tree(@PathVariable Long brandId) {
        return ResponseEntity.ok(groupTagService.tree(brandId));
    }

    @GetMapping("/by-brand/{brandId}/paged")
    @Operation(summary = "Группы тегов (пагинация)", description = "Требования: аутентификация. Данные в пределах контекста.")
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
    @Operation(summary = "Переименовать группу", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<GroupTagResponse> rename(
            @PathVariable Long groupTagId,
            @RequestParam String name
    ) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(groupTagService.rename(groupTagId, name));
    }

    // Единый апдейт: смена бренда, перенос, переименование — одним запросом
    @PutMapping("/{groupTagId}/full")
    @Operation(summary = "Полный апдейт группы", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<GroupTagResponse> updateFull(
            @PathVariable Long groupTagId,
            @Valid @RequestBody UpdateGroupTagRequest request
    ) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(groupTagService.updateGroupTag(groupTagId, request));
    }

    @PatchMapping("/{groupTagId}/move")
    @Operation(summary = "Переместить группу", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<GroupTagResponse> move(
            @PathVariable Long groupTagId,
            @RequestParam(required = false) Long parentId
    ) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(groupTagService.move(groupTagId, parentId));
    }

    @PatchMapping("/{groupTagId}/brand")
    @Operation(summary = "Сменить бренд у группы", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<GroupTagResponse> changeBrand(
            @PathVariable Long groupTagId,
            @RequestParam Long brandId
    ) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(groupTagService.changeBrand(groupTagId, brandId));
    }

    @GetMapping("/breadcrumbs/{groupTagId}")
    @Operation(summary = "Хлебные крошки", description = "Требования: аутентификация. Данные в пределах контекста.")
    public ResponseEntity<List<GroupTagResponse>> breadcrumbs(@PathVariable Long groupTagId) {
        return ResponseEntity.ok(groupTagService.breadcrumbs(groupTagId));
    }

    @DeleteMapping("/{groupTagId}")
    @Operation(summary = "Удалить группу (в архив)", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Void> deleteWithArchive(@PathVariable Long groupTagId) {
        rbacGuard.requireOwnerOrAdmin();
        groupTagService.deleteWithArchive(groupTagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/archive/purge")
    @Operation(summary = "Очистить архив групп", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Long> purgeArchive(@RequestParam(defaultValue = "90") int olderThanDays) {
        rbacGuard.requireOwnerOrAdmin();
        long deleted = groupTagService.purgeArchive(olderThanDays);
        return ResponseEntity.ok(deleted);
    }

    @GetMapping("/archive")
    @Operation(summary = "Архив групп бренда", description = "Требования: аутентификация. Данные в пределах контекста.")
    public ResponseEntity<java.util.List<GroupTagArchiveResponse>> listArchiveByBrand(@RequestParam Long brandId) {
        return ResponseEntity.ok(groupTagService.listArchiveByBrand(brandId));
    }

    @GetMapping("/archive/paged")
    @Operation(summary = "Архив групп бренда (пагинация)", description = "Требования: аутентификация. Данные в пределах контекста.")
    public ResponseEntity<Page<GroupTagArchiveResponse>> listArchiveByBrandPaged(
            @RequestParam Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "archivedAt,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction dir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortParts[0]));
        return ResponseEntity.ok(groupTagService.listArchiveByBrandPaged(brandId, pageable));
    }

    @PostMapping("/archive/{archiveId}/restore")
    @Operation(summary = "Восстановить группу из архива", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<GroupTagResponse> restoreFromArchive(
            @PathVariable Long archiveId,
            @RequestParam(required = false) Long targetParentId
    ) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(groupTagService.restoreGroupFromArchive(archiveId, targetParentId));
    }

    @DeleteMapping("/archive/{archiveId}")
    @Operation(summary = "Удалить запись архива группы", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Void> deleteGroupArchive(@PathVariable Long archiveId) {
        rbacGuard.requireOwnerOrAdmin();
        groupTagService.deleteGroupArchive(archiveId);
        return ResponseEntity.noContent().build();
    }
}
