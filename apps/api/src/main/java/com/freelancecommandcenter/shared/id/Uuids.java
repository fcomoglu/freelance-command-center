package com.freelancecommandcenter.shared.id;

import java.security.SecureRandom;
import java.util.UUID;

/** Generates time-ordered UUIDv7 identifiers (RFC 9562), so primary keys stay index-friendly. */
public final class Uuids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuids() {}

    public static UUID v7() {
        long millis = System.currentTimeMillis();
        long randA = RANDOM.nextInt(1 << 12);
        long randB = RANDOM.nextLong();
        long msb = (millis << 16) | (0x7L << 12) | randA;
        long lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
