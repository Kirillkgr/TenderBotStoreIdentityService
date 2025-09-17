package kirillzhdanov.identityservice.service;

import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.group.CreateGroupTagRequest;
import kirillzhdanov.identityservice.dto.group.GroupTagResponse;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupTagService {

    private final GroupTagRepository groupTagRepository;
    private final BrandRepository brandRepository;

    @Transactional
    public GroupTagResponse createGroupTag(CreateGroupTagRequest request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));

        GroupTag parent = null;
        if (request.getParentId() != null && request.getParentId() != 0) {
            parent = groupTagRepository.findByIdAndBrand(request.getParentId(), brand)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group tag not found with id: " + request.getParentId()));
        }

        // Check if group tag with same name already exists under the same parent
        if (groupTagRepository.existsByBrandAndNameAndParent(brand, request.getName(), parent)) {
            throw new IllegalArgumentException("Group tag with this name already exists in the specified location");
        }

        GroupTag groupTag = new GroupTag(request.getName(), brand, parent);
        groupTag = groupTagRepository.save(groupTag);

        return convertToDto(groupTag);
    }

    public List<GroupTagResponse> getGroupTagsByBrandAndParent(Long brandId, Long parentId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        List<GroupTag> groupTags;
        if (parentId == null || parentId == 0) {
            groupTags = groupTagRepository.findByBrandAndParentIsNull(brand);
        } else {
            groupTags = groupTagRepository.findByBrandAndParentId(brand, parentId);
        }

        return groupTags.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<GroupTagResponse> getGroupTagsTree(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));

        List<GroupTag> rootGroups = groupTagRepository.findByBrandAndParentIsNull(brand);
        return rootGroups.stream()
                .map(this::convertToDtoWithChildren)
                .collect(Collectors.toList());
    }

    private GroupTagResponse convertToDto(GroupTag groupTag) {
        return new GroupTagResponse(
                groupTag.getId(),
                groupTag.getName(),
                groupTag.getBrand().getId(),
                groupTag.getParent() != null ? groupTag.getParent().getId() : null,
                groupTag.getLevel()
        );
    }

    private GroupTagResponse convertToDtoWithChildren(GroupTag groupTag) {
        GroupTagResponse dto = convertToDto(groupTag);
        if (groupTag.getChildren() != null && !groupTag.getChildren().isEmpty()) {
            List<GroupTagResponse> children = groupTag.getChildren().stream()
                    .map(this::convertToDtoWithChildren)
                    .collect(Collectors.toList());
            dto.setChildren(children);
        }
        return dto;
    }
}
