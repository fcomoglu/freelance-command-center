package com.freelancecommandcenter.identity.api;

import com.freelancecommandcenter.shared.i18n.Language;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;

/** Published after a user and their workspace have been created. */
public record UserRegistered(UserId userId, WorkspaceId workspaceId, String email, Language language) {}
