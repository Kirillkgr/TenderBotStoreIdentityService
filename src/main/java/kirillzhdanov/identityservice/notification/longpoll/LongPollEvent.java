package kirillzhdanov.identityservice.notification.longpoll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LongPollEvent {
    private long id; // monotonically increasing per user
    private LongPollEventType type;

    // Common context
    private Long orderId;
    private Instant at;

    // Status change payload
    private String oldStatus;
    private String newStatus;

    // Courier message payload
    private String text;
}
