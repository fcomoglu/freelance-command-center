package com.freelancecommandcenter.notification.application.port.out;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.time.Instant;
import java.util.UUID;

/** Remembers which messages a workspace was notified about, so repeated events never notify twice. */
public interface MessageNotificationLog {

    /** Returns {@code false} when the workspace was already notified about the message. */
    boolean recordFirst(WorkspaceId workspaceId, UUID messageId, Instant at);
}
