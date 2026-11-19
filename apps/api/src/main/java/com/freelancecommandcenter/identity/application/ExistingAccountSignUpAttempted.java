package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.shared.id.UserId;

/**
 * Someone tried to sign up with the address of an existing account. Internal to the identity module; public only so
 * the event outbox can deserialize it.
 */
public record ExistingAccountSignUpAttempted(UserId userId) {}
