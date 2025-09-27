package kirillzhdanov.identityservice.notification.longpoll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LongPollEnvelope {
    private List<LongPollEvent> events;
    private long nextSince;   // client should pass this value as 'since' in the next poll
    private boolean hasMore;  // there are more events buffered server-side beyond this page
    private int unreadCount;  // approximate unread count for badge
}
