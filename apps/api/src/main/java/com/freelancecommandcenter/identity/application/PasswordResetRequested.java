package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.shared.id.UserId;

/**
 * A user asked for a password reset link. Internal to the identity module; public only so the event outbox can
 * deserialize it.
 */
public record PasswordResetRequested(UserId userId) {}
