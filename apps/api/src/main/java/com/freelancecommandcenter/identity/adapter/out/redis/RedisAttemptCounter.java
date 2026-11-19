package com.freelancecommandcenter.identity.adapter.out.redis;

import com.freelancecommandcenter.identity.application.port.out.AttemptCounter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/** Counters in Redis. Keys are hashed so e-mail addresses and IP addresses are not stored in clear text. */
@Component
class RedisAttemptCounter implements AttemptCounter {

    private static final String PREFIX = "fcc:identity:attempts:";

    // Increment and expiry in one atomic step, so a counter can never be left without an expiry.
    private static final RedisScript<Long> INCREMENT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return count""", Long.class);

    private final StringRedisTemplate redis;

    RedisAttemptCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long increment(String key, Duration window) {
        Long count = redis.execute(INCREMENT, List.of(redisKey(key)), String.valueOf(window.toMillis()));
        return count == null ? 0 : count;
    }

    @Override
    public long count(String key) {
        String value = redis.opsForValue().get(redisKey(key));
        return value == null ? 0 : Long.parseLong(value);
    }

    @Override
    public void reset(String key) {
        redis.delete(redisKey(key));
    }

    private static String redisKey(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
