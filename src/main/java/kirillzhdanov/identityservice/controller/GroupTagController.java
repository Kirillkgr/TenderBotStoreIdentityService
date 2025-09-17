package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.service.GroupTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
