package kirillzhdanov.identityservice.model.order;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    // true = клиент -> админ; false = админ -> клиент
    @Column(name = "from_client", nullable = false)
    private boolean fromClient;

    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
