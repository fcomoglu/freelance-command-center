package com.freelancecommandcenter.notification.application.port.out;

import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.time.Instant;

/** Remembers which jobs a workspace was notified about, so repeated events never notify twice. */
public interface JobNotificationLog {

    /** Returns {@code false} when the workspace was already notified about the job. */
    boolean recordFirst(WorkspaceId workspaceId, JobId jobId, Instant at);
}
