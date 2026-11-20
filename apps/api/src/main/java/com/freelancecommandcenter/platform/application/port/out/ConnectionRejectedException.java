package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.shared.platform.PlatformCode;

/** The platform refused an authorization code or a connected account's tokens; the user has to connect again. */
public class ConnectionRejectedException extends PlatformUnavailableException {

    private static final long serialVersionUID = 1L;

    public ConnectionRejectedException(PlatformCode platform, String message, Throwable cause) {
        super(platform, message, cause);
    }
}
