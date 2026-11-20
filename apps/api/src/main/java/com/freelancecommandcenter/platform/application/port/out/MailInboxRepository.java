package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.domain.MailInbox;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.util.Optional;

public interface MailInboxRepository {

    Optional<MailInbox> findByWorkspace(WorkspaceId workspaceId);

    Optional<MailInbox> findByLocalPart(String localPart);

    /** Stores the inbox of its workspace, replacing an earlier address. */
    void save(MailInbox inbox);
}
