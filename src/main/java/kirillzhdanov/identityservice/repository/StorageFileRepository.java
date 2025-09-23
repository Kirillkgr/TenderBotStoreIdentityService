package kirillzhdanov.identityservice.repository;

import kirillzhdanov.identityservice.model.StorageFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {
    List<StorageFile> findByOwnerTypeAndOwnerId(String ownerType, Long ownerId);
}
