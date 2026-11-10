package com.smartwater.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small key/value helper used for JWT revocation, rate limiting, scheduler locks and OAuth sign-up tickets.
 * <ul>
 *   <li>app.redis.enabled=true  -> Redis (shared by every backend instance)</li>
 *   <li>app.redis.enabled=false -> in-memory map (fine for one instance, dev and tests)</li>
 * </ul>
 * Redis problems never break the API: every operation fails open (logs a warning and returns a harmless default).
 */
@Component
@Slf4j
public class KeyValueStore {
    private record Entry(String value, long expiresAt, long count) {}

    private final StringRedisTemplate redis;
    private final boolean useRedis;
    private final Map<String, Entry> local = new ConcurrentHashMap<>();

    public KeyValueStore(StringRedisTemplate redis, @Value("${app.redis.enabled:false}") boolean useRedis) {
        this.redis = redis;
        this.useRedis = useRedis && redis != null;
    }

    // ---------------------------------------------------------------- values
    public void put(String key, String value, Duration ttl) {
        if (useRedis) {
            try { redis.opsForValue().set(key, value, ttl); } catch (RuntimeException e) { warn("put", e); }
            return;
        }
        long now = System.currentTimeMillis();
        sweep(now);
        local.put(key, new Entry(value, now + ttl.toMillis(), 0));
    }

    public Optional<String> get(String key) {
        if (useRedis) {
            try { return Optional.ofNullable(redis.opsForValue().get(key)); }
            catch (RuntimeException e) { warn("get", e); return Optional.empty(); }
        }
        Entry e = local.get(key);
        if (e == null) return Optional.empty();
        if (e.expiresAt() < System.currentTimeMillis()) { local.remove(key, e); return Optional.empty(); }
        return Optional.ofNullable(e.value());
    }

    public boolean exists(String key) { return get(key).isPresent(); }

    public void delete(String key) {
        if (useRedis) {
            try { redis.delete(key); } catch (RuntimeException e) { warn("delete", e); }
            return;
        }
        local.remove(key);
    }

    // ---------------------------------------------------------------- locks and counters
    /** Atomic "set if absent" with expiry: true for the one caller that wins (used as a distributed lock). */
    public boolean setIfAbsent(String key, Duration ttl) {
        if (useRedis) {
            try { return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "1", ttl)); }
            catch (RuntimeException e) { warn("setIfAbsent", e); return true; }   // fail open: better to run twice than never
        }
        long now = System.currentTimeMillis();
        boolean[] won = {false};
        local.compute(key, (k, cur) -> {
            if (cur == null || cur.expiresAt() < now) { won[0] = true; return new Entry("1", now + ttl.toMillis(), 0); }
            return cur;
        });
        return won[0];
    }

    /** Fixed-window counter: returns how many times the key was incremented in the current window. */
    public long increment(String key, Duration window) {
        if (useRedis) {
            try {
                Long n = redis.opsForValue().increment(key);
                if (n != null && n == 1L) redis.expire(key, window);
                return n == null ? 0 : n;
            } catch (RuntimeException e) { warn("increment", e); return 0; }       // fail open
        }
        long now = System.currentTimeMillis();
        sweep(now);
        long[] result = {0};
        local.compute(key, (k, cur) -> {
            if (cur == null || cur.expiresAt() < now) { result[0] = 1; return new Entry("", now + window.toMillis(), 1); }
            result[0] = cur.count() + 1;
            return new Entry("", cur.expiresAt(), result[0]);
        });
        return result[0];
    }

    private void sweep(long now) {
        if (local.size() > 10_000) local.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
    }

    private void warn(String op, RuntimeException e) {
        log.warn("Redis {} failed, continuing without it: {}", op, e.getMessage());
    }
}
