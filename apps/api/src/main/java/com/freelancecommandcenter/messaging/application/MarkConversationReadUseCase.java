package com.freelancecommandcenter.messaging.application;

import com.freelancecommandcenter.messaging.application.port.out.ConversationRepository;
import com.freelancecommandcenter.messaging.domain.Conversation;
import com.freelancecommandcenter.shared.id.ConversationId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clears the unread count of a conversation the user opened in the application. */
@Service
public class MarkConversationReadUseCase {

    private final ConversationRepository conversations;

    MarkConversationReadUseCase(ConversationRepository conversations) {
        this.conversations = conversations;
    }

    /** @throws com.freelancecommandcenter.shared.error.DomainException when the workspace has no such conversation */
    @Transactional
    public void markRead(WorkspaceId workspaceId, ConversationId conversationId) {
        // Locked like for a new message, so a message arriving meanwhile is not overwritten by this update.
        Conversation conversation =
                conversations.lock(workspaceId, conversationId).orElseThrow(ConversationQuery::notFound);
        conversation.markRead();
        conversations.save(conversation);
    }
}
