package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.MailInboxRepository;
import com.freelancecommandcenter.platform.domain.MailInbox;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The workspace's inbox address for forwarded platform e-mails. */
@Service
public class MailInboxUseCase {

    private final MailInboxRepository inboxes;
    private final String domain;

    MailInboxUseCase(MailInboxRepository inboxes, PlatformProperties properties) {
        this.inboxes = inboxes;
        this.domain = properties.mail().inboundDomain();
    }

    /** The current address; the first call creates it. */
    @Transactional
    public String address(WorkspaceId workspaceId) {
        MailInbox inbox = inboxes.findByWorkspace(workspaceId).orElseGet(() -> {
            MailInbox created = MailInbox.newFor(workspaceId);
            inboxes.save(created);
            return created;
        });
        return inbox.address(domain);
    }

    /** Replaces the address; mail to the old one is no longer accepted. */
    @Transactional
    public String renew(WorkspaceId workspaceId) {
        MailInbox inbox = MailInbox.newFor(workspaceId);
        inboxes.save(inbox);
        return inbox.address(domain);
    }
}
