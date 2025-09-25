package kirillzhdanov.identityservice.notification.longpoll;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.concurrent.CompletableFuture;

import kirillzhdanov.identityservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/notifications/longpoll")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class LongPollController {

    private final LongPollService longPollService;
    private final UserRepository userRepository;

    @GetMapping
    public CompletableFuture<ResponseEntity<?>> poll(
            @RequestParam(name = "since", required = false, defaultValue = "0") long since,
            @RequestParam(name = "timeoutMs", required = false, defaultValue = "60000") long timeoutMs,
            @RequestParam(name = "maxBatch", required = false, defaultValue = "50") int maxBatch,
            Authentication authentication
    ) {
        try {
            log.info("[LP] controller poll ENTER since={} timeoutMs={} maxBatch={} principal={} isAuth={}",
                    since, timeoutMs, maxBatch,
                    authentication != null ? authentication.getName() : null,
                    authentication != null && authentication.isAuthenticated());
            if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
                // Открытый доступ: idle-ответ без событий (204), чтобы на фронте не было 401
                log.info("[LP] controller poll UNAUTH since={} -> 204", since);
                return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
            }
            Long userId = resolveUserId(authentication);
            return longPollService.poll(userId, since, timeoutMs, maxBatch)
                    .thenApply(envelope -> {
                        if (envelope == null || envelope.getEvents() == null || envelope.getEvents().isEmpty()) {
                            // idle-ответ: 204 No Content
                            log.info("[LP] controller poll OK idle userId={} since={} -> 204", userId, since);
                            return ResponseEntity.noContent().build();
                        }
                        log.info("[LP] controller poll OK events userId={} since={} -> 200 events={} nextSince={} hasMore={}",
                                userId, since, envelope.getEvents().size(), envelope.getNextSince(), envelope.isHasMore());
                        return ResponseEntity.ok(envelope);
                    })
                    .exceptionally(ex -> {
                        log.error("[LP] controller poll EXCEPTION since={} -> 204 (mapped)", since, ex);
                        return ResponseEntity.noContent().build();
                    });
        } catch (Exception ex) {
            log.error("[LP] controller poll OUTER EXCEPTION since={} -> 204 (mapped)", since, ex);
            return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
        }
    }

    @PostMapping("/ack")
    public ResponseEntity<Void> ack(@RequestBody LongPollAckRequest req, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
                // Открытый доступ: без аутентификации просто no-op, чтобы не получать 401 на фронте
                log.info("[LP] controller ack UNAUTH lastReceivedId={} -> 204", req != null ? req.getLastReceivedId() : null);
                return ResponseEntity.noContent().build();
            }
            Long userId = resolveUserId(authentication);
            longPollService.ack(userId, req.getLastReceivedId());
            log.info("[LP] controller ack OK userId={} lastReceivedId={} -> 204", userId, req.getLastReceivedId());
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("[LP] controller ack EXCEPTION -> 204 (mapped)", ex);
            return ResponseEntity.noContent().build();
        }
    }

    private Long resolveUserId(Authentication authentication) {
        // Корректно резолвим числовой userId по username, чтобы он совпадал с publish(userId,...)
        try {
            if (authentication == null) return 0L;
            String name = authentication.getName();
            if (name == null) return 0L;
            // сначала пробуем как id
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException ignore) {
            }
            // иначе ищем по username
            return userRepository.findByUsername(name)
                    .map(u -> u.getId())
                    .orElseGet(() -> {
                        log.warn("LongPoll: cannot resolve userId by username='{}', fallback to hash queue", name);
                        return (long) name.hashCode();
                    });
        } catch (Exception ex) {
            log.error("LongPoll: resolveUserId failed", ex);
            try {
                return (long) String.valueOf(authentication != null ? authentication.getName() : "").hashCode();
            } catch (Exception ignored) {
                return 0L;
            }
        }
    }
}
