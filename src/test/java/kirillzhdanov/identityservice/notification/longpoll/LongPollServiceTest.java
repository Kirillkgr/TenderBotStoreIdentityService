package kirillzhdanov.identityservice.notification.longpoll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class LongPollServiceTest {

    private LongPollService service;

    @BeforeEach
    void setUp() {
        service = new LongPollService();
    }

    @Test
    void pollReturnsIdle204EnvelopeOnTimeout() throws Exception {
        long userId = 1L;
        long since = 0L;
        CompletableFuture<LongPollEnvelope> f = service.poll(userId, since, 200, 50);
        LongPollEnvelope env = f.get();
        assertNotNull(env);
        assertTrue(env.getEvents().isEmpty());
        assertEquals(since, env.getNextSince());
        assertFalse(env.isHasMore());
    }

    @Test
    void publishThenPollDeliversEventsAndIncreasesNextSince() throws Exception {
        long userId = 1L;
        // publish couple of events
        service.publishClientMessage(userId, 100L, "hello");
        service.publishCourierMessage(userId, 100L, "world");

        LongPollEnvelope env = service.poll(userId, 0, 10, 50).get();
        assertEquals(2, env.getEvents().size());
        assertTrue(env.getNextSince() > 0);
        assertFalse(env.isHasMore());
    }

    @Test
    void pollParksAndCompletesWhenEventArrives() throws Exception {
        long userId = 1L;
        CompletableFuture<LongPollEnvelope> f = service.poll(userId, 0, 2000, 50);
        assertFalse(f.isDone());
        // after a short delay, publish event
        Thread.sleep(50);
        service.publishStatusChanged(userId, 101L, "QUEUED", "PREPARING");
        LongPollEnvelope env = f.get();
        assertEquals(1, env.getEvents().size());
        assertTrue(env.getNextSince() >= env.getEvents().get(0).getId());
    }

    @Test
    void ackTrimsBufferUpToLastReceived() throws Exception {
        long userId = 1L;
        service.publishClientMessage(userId, 1L, "a");
        service.publishClientMessage(userId, 1L, "b");
        LongPollEnvelope env = service.poll(userId, 0, 10, 50).get();
        long last = env.getNextSince();
        // ack up to last
        service.ack(userId, last);
        // subsequent poll should be idle if no new events
        LongPollEnvelope env2 = service.poll(userId, last, 30, 50).get();
        assertTrue(env2.getEvents().isEmpty());
        assertEquals(last, env2.getNextSince());
    }

    @Test
    void paginationMaxBatchRespectedAndHasMoreSet() throws Exception {
        long userId = 1L;
        for (int i = 0; i < 10; i++) service.publishClientMessage(userId, 1L, "m" + i);
        LongPollEnvelope env = service.poll(userId, 0, 10, 3).get();
        assertEquals(3, env.getEvents().size());
        assertTrue(env.isHasMore());
        long next = env.getNextSince();
        LongPollEnvelope rest = service.poll(userId, next, 10, 50).get();
        assertEquals(7, rest.getEvents().size());
        assertFalse(rest.isHasMore());
    }
}
