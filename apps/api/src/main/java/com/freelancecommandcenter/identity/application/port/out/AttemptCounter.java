package com.freelancecommandcenter.identity.application.port.out;

import java.time.Duration;

/** Counts attempts per key in a fixed time window that starts with the first attempt. */
public interface AttemptCounter {

    /** Records one attempt and returns the number of attempts in the current window, including this one. */
    long increment(String key, Duration window);

    long count(String key);

    void reset(String key);
}
