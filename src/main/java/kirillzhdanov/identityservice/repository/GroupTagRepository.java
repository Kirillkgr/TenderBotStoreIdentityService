package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GroupTagRepository extends JpaRepository<GroupTag, Long> {
    List<GroupTag> findByBrandAndParentIsNull(Brand brand);
    Page<GroupTag> findByBrandAndParentIsNull(Brand brand, Pageable pageable);
    
    List<GroupTag> findByBrandAndParentId(Brand brand, Long parentId);
    Page<GroupTag> findByBrandAndParentId(Brand brand, Long parentId, Pageable pageable);

    boolean existsByBrandAndNameAndParent(Brand brand, String name, GroupTag parent);
    
    Optional<GroupTag> findByIdAndBrand(Long id, Brand brand);

    @Query("SELECT gt FROM GroupTag gt WHERE gt.brand = :brand AND gt.path LIKE CONCAT(:prefix, '%')")
    List<GroupTag> findSubtreeByPathPrefix(@Param("brand") Brand brand, @Param("prefix") String prefix);

    @Query("SELECT gt FROM GroupTag gt WHERE gt.brand = :brand ORDER BY gt.level ASC, gt.id ASC")
    List<GroupTag> findAllByBrandOrdered(@Param("brand") Brand brand);
}
