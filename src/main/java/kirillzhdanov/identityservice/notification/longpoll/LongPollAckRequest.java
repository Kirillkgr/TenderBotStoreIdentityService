package kirillzhdanov.identityservice.notification.longpoll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LongPollAckRequest {
    private long lastReceivedId; // client acknowledges having received all events up to and including this id
}
