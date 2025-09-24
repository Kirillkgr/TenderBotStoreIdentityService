package kirillzhdanov.identityservice.model.order;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Клиент, оформивший заказ (покупатель)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    // Бренд, у которого оформлен заказ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "total", precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 64, nullable = false)
    private OrderStatus status = OrderStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", length = 32, nullable = false)
    private DeliveryMode deliveryMode = DeliveryMode.PICKUP;

    // Snapshots to keep chosen delivery address or pickup point at the time of ordering
    @Column(name = "address_snapshot", columnDefinition = "TEXT")
    private String addressSnapshot;

    @Column(name = "pickup_snapshot", columnDefinition = "TEXT")
    private String pickupSnapshot;

    @Column(name = "comment", length = 2000)
    private String comment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
