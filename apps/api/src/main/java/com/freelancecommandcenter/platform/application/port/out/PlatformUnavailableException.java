package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.shared.platform.PlatformCode;

/** A platform could not be read: network failure, rate limit, rejected credentials or an unexpected response. */
public class PlatformUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PlatformUnavailableException(PlatformCode platform, String message, Throwable cause) {
        super(platform.value() + ": " + message, cause);
    }
}
