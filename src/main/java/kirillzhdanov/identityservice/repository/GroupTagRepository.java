package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupTagRepository extends JpaRepository<GroupTag, Long> {
    List<GroupTag> findByBrandAndParentIsNull(Brand brand);
    
    List<GroupTag> findByBrandAndParentId(Brand brand, Long parentId);
    
    @Query("SELECT gt FROM GroupTag gt WHERE gt.brand = :brand AND (gt.parent.id = :parentId OR :parentId IS NULL)")
    List<GroupTag> findByBrandAndParentIdOrRoot(@Param("brand") Brand brand, 
                                              @Param("parentId") Long parentId);
                                              
    boolean existsByBrandAndNameAndParent(Brand brand, String name, GroupTag parent);
    
    Optional<GroupTag> findByIdAndBrand(Long id, Brand brand);
}
