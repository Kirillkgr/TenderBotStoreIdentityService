package kirillzhdanov.identityservice.notification.longpoll;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * In-memory long-poll broker per user.
 * - Stores events with monotonically increasing IDs per user
 * - Supports long-poll with timeout and pagination (maxBatch)
 * - Supports ACK to trim delivered events
 * <p>
 * Important: This is an in-memory implementation intended for MVP and tests.
 * It can be extracted behind an interface and replaced by a persistent broker later.
 */
@Service
@Slf4j
public class LongPollService {

    private static final int MAX_BUFFER_PER_USER = 1000; // safety cap
    private final Map<Long, UserQueue> queues = new ConcurrentHashMap<>();
    // Планировщик для таймаутов long-poll (легковесный пул)
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "longpoll-timeout");
        t.setDaemon(true);
        return t;
    });

    private UserQueue q(Long userId) {
        return queues.computeIfAbsent(userId, k -> new UserQueue());
    }

    public void publishStatusChanged(Long userId, Long orderId, String oldStatus, String newStatus) {
        LongPollEvent evt = LongPollEvent.builder()
                .type(LongPollEventType.ORDER_STATUS_CHANGED)
                .orderId(orderId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .at(Instant.now())
                .build();
        log.debug("[LP] publishStatusChanged userId={} orderId={} {}->{}", userId, orderId, oldStatus, newStatus);
        enqueue(userId, evt);
    }

    public void publishCourierMessage(Long userId, Long orderId, String text, Long messageId) {
        LongPollEvent evt = LongPollEvent.builder()
                .type(LongPollEventType.COURIER_MESSAGE)
                .orderId(orderId)
                .messageId(messageId)
                .text(text)
                .at(Instant.now())
                .build();
        log.debug("[LP] publishCourierMessage userId={} orderId={} text='{}'", userId, orderId, text);
        enqueue(userId, evt);
    }

    public void publishClientMessage(Long userId, Long orderId, String text, Long messageId) {
        LongPollEvent evt = LongPollEvent.builder()
                .type(LongPollEventType.CLIENT_MESSAGE)
                .orderId(orderId)
                .messageId(messageId)
                .text(text)
                .at(Instant.now())
                .build();
        log.debug("[LP] publishClientMessage userId={} orderId={} text='{}'", userId, orderId, text);
        enqueue(userId, evt);
    }

    public void publishNewOrder(Long userId, Long orderId) {
        LongPollEvent evt = LongPollEvent.builder()
                .type(LongPollEventType.NEW_ORDER)
                .orderId(orderId)
                .at(Instant.now())
                .build();
        log.debug("[LP] publishNewOrder userId={} orderId={}", userId, orderId);
        enqueue(userId, evt);
    }

    private void enqueue(Long userId, LongPollEvent evt) {
        UserQueue uq = q(userId);
        synchronized (uq) {
            long id = ++uq.lastId;
            evt.setId(id);
            uq.buffer.add(evt);
            // trim if exceeds cap
            int overflow = uq.buffer.size() - MAX_BUFFER_PER_USER;
            if (overflow > 0) {
                uq.buffer.subList(0, overflow).clear();
            }
            // wake waiters
            if (log.isDebugEnabled()) {
                log.debug("[LP] enqueue userId={} id={} type={} bufferSize={} waiters={}", userId, id, evt.getType(), uq.buffer.size(), uq.waiters.size());
            }
            if (!uq.waiters.isEmpty()) {
                List<CompletableFuture<LongPollEnvelope>> copy = new ArrayList<>(uq.waiters);
                uq.waiters.clear();
                for (CompletableFuture<LongPollEnvelope> w : copy) {
                    safeCompleteNow(uq, w, 0, 50);
                }
            }
        }
    }

    public void publish(Long userId, LongPollEvent evt) {
        enqueue(userId, evt);
    }

    /**
     * Long poll for events after 'since' (exclusive). If none, wait up to timeoutMs.
     */
    public CompletableFuture<LongPollEnvelope> poll(Long userId, long since, long timeoutMs, int maxBatch) {
//        log.info("[LP] poll enter userId={} since={} timeoutMs={} maxBatch={}", userId, since, timeoutMs, maxBatch);
        Objects.requireNonNull(userId, "userId");
        if (timeoutMs <= 0) timeoutMs = 60000; // default 60s
        if (maxBatch <= 0 || maxBatch > 200) maxBatch = 50;
        UserQueue uq = q(userId);
        CompletableFuture<LongPollEnvelope> promise = new CompletableFuture<>();
        synchronized (uq) {
            // If we already have events beyond 'since', return immediately
            List<LongPollEvent> events;
            try {
                events = uq.buffer.stream()
                        .filter(e -> e.getId() > since)
                        .limit(maxBatch)
                        .toList();
            } catch (Exception ex) {
                // На всякий случай — не даём исключению уйти наружу
                events = List.of();
            }
            // Special case: initial connect (since<=0) — return last up to 10 events to provide context
            if ((since <= 0) && events.isEmpty() && !uq.buffer.isEmpty()) {
                int fromIdx = Math.max(0, uq.buffer.size() - 10);
                events = new ArrayList<>(uq.buffer.subList(fromIdx, uq.buffer.size()));
            }
            if (!events.isEmpty()) {
                long nextSince = events.getLast().getId();
                boolean hasMore = uq.buffer.stream().anyMatch(e -> e.getId() > nextSince);
                log.info("[LP] poll immediate userId={} since={} -> nextSince={} events={} hasMore={} bufferSize={} waiters={}",
                        userId, since, nextSince, events.size(), hasMore, uq.buffer.size(), uq.waiters.size());
                promise.complete(LongPollEnvelope.builder()
                        .events(Collections.unmodifiableList(events))
                        .nextSince(nextSince)
                        .hasMore(hasMore)
                        .unreadCount(uq.buffer.size())
                        .build());
                return promise;
            }
            // Otherwise, park waiter and set a timeout fallback
            uq.waiters.add(promise);
//            log.info("[LP] poll parked userId={} since={} waitersNow={}", userId, since, uq.waiters.size());
        }
        // timeout: complete with empty envelope keeping same 'since' (через планировщик)
        long finalTimeoutMs = timeoutMs;
        final ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            try {
                UserQueue uq2 = q(userId);
                synchronized (uq2) {
                    if (promise.isDone()) return;
                    uq2.waiters.remove(promise);
//                    log.info("[LP] poll timeout userId={} since={} waitersAfterRm={} bufferSize={}", userId, since, uq2.waiters.size(), uq2.buffer.size());
                    promise.complete(LongPollEnvelope.builder()
                            .events(List.of())
                            .nextSince(since)
                            .hasMore(false)
                            .unreadCount(uq2.buffer.size())
                            .build());
                }
            } catch (Exception ignored) {
            }
        }, finalTimeoutMs, TimeUnit.MILLISECONDS);

        // Если промис завершится раньше — отменяем таймаут
        promise.whenComplete((res, err) -> {
            try {
                timeoutTask.cancel(false);
            } catch (Exception ignored) {
            }
            if (err != null) {
                log.error("[LP] poll complete exceptionally userId={} since={} err={}", userId, since, err.toString());
            } else if (res != null) {
                log.info("[LP] poll complete userId={} nextSince={} events={} hasMore={}", userId, res.getNextSince(),
                        res.getEvents() != null ? res.getEvents().size() : 0, res.isHasMore());
            } else {
                log.warn("[LP] poll complete userId={} with null result", userId);
            }
        });
        return promise;
    }

    public void ack(Long userId, long lastReceivedId) {
        Objects.requireNonNull(userId, "userId");
        UserQueue uq = q(userId);
        synchronized (uq) {
            // drop all <= lastReceivedId
            int idx = -1;
            for (int i = uq.buffer.size() - 1; i >= 0; i--) {
                if (uq.buffer.get(i).getId() <= lastReceivedId) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                uq.buffer.subList(0, idx + 1).clear();
                log.debug("[LP] ack userId={} lastReceivedId={} newBufferSize={}", userId, lastReceivedId, uq.buffer.size());
            }
        }
    }

    private void safeCompleteNow(UserQueue uq, CompletableFuture<LongPollEnvelope> w, long since, int maxBatch) {
        if (w.isDone()) return;
        List<LongPollEvent> events = uq.buffer.stream()
                .filter(e -> e.getId() > since)
                .limit(maxBatch)
                .toList();
        long nextSince = events.isEmpty() ? since : events.getLast().getId();
        boolean hasMore = !events.isEmpty() && uq.buffer.stream().anyMatch(e -> e.getId() > nextSince);
        w.complete(LongPollEnvelope.builder()
                .events(events)
                .nextSince(nextSince)
                .hasMore(hasMore)
                .unreadCount(uq.buffer.size())
                .build());
    }

    private static class UserQueue {
        // sorted by id ascending
        final List<LongPollEvent> buffer = new ArrayList<>();
        // pending waiters
        final List<CompletableFuture<LongPollEnvelope>> waiters = new CopyOnWriteArrayList<>();
        long lastId = 0L;
    }
}
