package kirillzhdanov.identityservice.model.master;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.pickup.PickupPoint;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_membership",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_um_user_brand", columnNames = {"user_id", "brand_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_id", nullable = false)
    private MasterAccount master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_point_id")
    private PickupPoint pickupPoint;

    @Column(name = "role", length = 32, nullable = false)
    private String role; // Owner/Admin/Cashier/Cook/Client

    @Column(name = "status", length = 32)
    private String status; // ACTIVE/INVITED/BLOCKED

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (twoFactorEnabled == null) twoFactorEnabled = Boolean.FALSE;
        if (status == null) status = "ACTIVE";
        if (role == null) role = "CLIENT";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
