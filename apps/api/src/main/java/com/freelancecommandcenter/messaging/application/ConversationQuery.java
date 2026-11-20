package com.freelancecommandcenter.messaging.application;

import com.freelancecommandcenter.messaging.application.port.out.ConversationRepository;
import com.freelancecommandcenter.messaging.domain.Conversation;
import com.freelancecommandcenter.messaging.domain.Message;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import com.freelancecommandcenter.shared.id.ConversationId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.pagination.Page;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The workspace's inbox: conversations with the newest message first, and the messages of one conversation. */
@Service
public class ConversationQuery {

    private final ConversationRepository conversations;

    ConversationQuery(ConversationRepository conversations) {
        this.conversations = conversations;
    }

    @Transactional(readOnly = true)
    public Page<Conversation> list(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit) {
        return Page.of(
                conversations.findRecent(workspaceId, after, limit + 1),
                limit,
                conversation -> new KeysetCursor(
                        conversation.lastMessageAt(), conversation.id().value()));
    }

    /** @throws DomainException {@code messaging.conversation_not_found} when the workspace has no such conversation */
    @Transactional(readOnly = true)
    public Page<Message> messages(
            WorkspaceId workspaceId, ConversationId conversationId, Optional<KeysetCursor> after, int limit) {
        Conversation conversation =
                conversations.find(workspaceId, conversationId).orElseThrow(ConversationQuery::notFound);
        return Page.of(
                conversations.findMessages(conversation.id(), after, limit + 1),
                limit,
                message -> new KeysetCursor(message.sentAt(), message.id()));
    }

    static DomainException notFound() {
        return new DomainException(
                ErrorKind.NOT_FOUND, "messaging.conversation_not_found", "The workspace has no such conversation");
    }
}
