package com.freelancecommandcenter.identity.api;

import com.freelancecommandcenter.shared.id.UserId;

/** Whether users have confirmed their e-mail address, for features that require it. */
public interface EmailVerification {

    /** {@code false} also for a user that does not exist. */
    boolean isVerified(UserId userId);
}
