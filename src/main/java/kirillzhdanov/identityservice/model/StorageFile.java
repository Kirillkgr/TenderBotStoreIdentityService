package kirillzhdanov.identityservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "storage_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024)
    private String path; // S3 key

    @Column(nullable = false)
    private String purpose; // e.g., USER_AVATAR, PRODUCT_IMAGE, TAG_IMAGE

    @Column(name = "usage_type")
    private String usageType; // optional subtype

    @Column(name = "owner_type")
    private String ownerType; // e.g., USER, PRODUCT, TAG_GROUP

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
