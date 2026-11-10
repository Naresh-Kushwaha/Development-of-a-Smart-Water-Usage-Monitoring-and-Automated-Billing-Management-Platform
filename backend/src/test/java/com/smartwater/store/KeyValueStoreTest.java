package com.smartwater.store;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** In-memory mode (app.redis.enabled=false): same contract as the Redis mode, no server needed. */
class KeyValueStoreTest {
    private final KeyValueStore store = new KeyValueStore(null, false);

    @Test
    void putGetExistsAndDelete() {
        store.put("k", "v", Duration.ofMinutes(1));

        assertEquals("v", store.get("k").orElseThrow());
        assertTrue(store.exists("k"));

        store.delete("k");
        assertFalse(store.exists("k"));
        assertTrue(store.get("missing").isEmpty());
    }

    @Test
    void valuesExpire() throws Exception {
        store.put("short", "v", Duration.ofMillis(20));
        Thread.sleep(80);
        assertTrue(store.get("short").isEmpty());
    }

    @Test
    void setIfAbsent_onlyTheFirstCallerWinsTheLock() {
        assertTrue(store.setIfAbsent("lock:job", Duration.ofMinutes(1)));
        assertFalse(store.setIfAbsent("lock:job", Duration.ofMinutes(1)));
        assertTrue(store.setIfAbsent("lock:other", Duration.ofMinutes(1)));
    }

    @Test
    void setIfAbsent_canBeTakenAgainAfterItExpires() throws Exception {
        assertTrue(store.setIfAbsent("lock", Duration.ofMillis(20)));
        Thread.sleep(80);
        assertTrue(store.setIfAbsent("lock", Duration.ofMinutes(1)));
    }

    @Test
    void increment_countsWithinTheWindowAndRestartsAfterIt() throws Exception {
        assertEquals(1, store.increment("rl:ip", Duration.ofMillis(60)));
        assertEquals(2, store.increment("rl:ip", Duration.ofMillis(60)));
        assertEquals(1, store.increment("rl:other", Duration.ofMinutes(1)));

        Thread.sleep(120);
        assertEquals(1, store.increment("rl:ip", Duration.ofMinutes(1)));
    }
}
