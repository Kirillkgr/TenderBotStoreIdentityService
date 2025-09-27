package kirillzhdanov.identityservice.model.order;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.User;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(nullable = false)
    private Integer rating; // 1..5

    @Column(length = 4000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
