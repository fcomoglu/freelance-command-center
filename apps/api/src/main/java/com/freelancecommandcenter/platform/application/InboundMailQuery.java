package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.InboundMailStore;
import com.freelancecommandcenter.platform.domain.InboundMail;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.pagination.Page;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The mails a workspace's inbox received, newest first, so the user can check that forwarding works. */
@Service
public class InboundMailQuery {

    private final InboundMailStore mails;

    InboundMailQuery(InboundMailStore mails) {
        this.mails = mails;
    }

    @Transactional(readOnly = true)
    public Page<InboundMail> list(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit) {
        return Page.of(
                mails.findRecent(workspaceId, after, limit + 1),
                limit,
                mail -> new KeysetCursor(mail.receivedAt(), mail.id()));
    }
}
