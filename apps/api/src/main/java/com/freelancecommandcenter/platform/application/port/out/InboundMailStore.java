package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.domain.InboundMail;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import java.util.List;
import java.util.Optional;

public interface InboundMailStore {

    /**
     * Stores the mail with its raw message.
     *
     * @return {@code false} when the workspace already has a mail with the same Message-ID
     */
    boolean save(InboundMail mail, byte[] raw);

    /** Mails of the workspace, newest first (then by id), after {@code after} when given. */
    List<InboundMail> findRecent(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit);
}
