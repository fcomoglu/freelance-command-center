package com.freelancecommandcenter.notification.domain;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;

/** The value is not an Expo push token ({@code ExponentPushToken[...]}). */
public class InvalidPushTokenException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidPushTokenException() {
        super(ErrorKind.INVALID, "notification.invalid_push_token", "Not an Expo push token");
    }
}
