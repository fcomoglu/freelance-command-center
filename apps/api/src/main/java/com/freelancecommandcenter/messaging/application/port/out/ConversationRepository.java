package com.freelancecommandcenter.messaging.application.port.out;

import com.freelancecommandcenter.messaging.domain.Conversation;
import com.freelancecommandcenter.messaging.domain.Message;
import com.freelancecommandcenter.shared.id.ConversationId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    /**
     * Returns the stored conversation with the workspace, platform and external id of {@code fresh}, creating it from
     * {@code fresh} when there is none, and locks it until the transaction ends, so concurrent messages of one
     * conversation are applied one after the other.
     */
    Conversation lockOrCreate(Conversation fresh);

    /** Like {@link #find}, and locks the conversation until the transaction ends. */
    Optional<Conversation> lock(WorkspaceId workspaceId, ConversationId conversationId);

    void save(Conversation conversation);

    /** @return {@code false} when the conversation already has a message with the same external id */
    boolean addMessage(ConversationId conversationId, Message message);

    Optional<Conversation> find(WorkspaceId workspaceId, ConversationId conversationId);

    /** Conversations of the workspace, newest message first (then by id), after {@code after} when given. */
    List<Conversation> findRecent(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit);

    /** Messages of the conversation, newest first (then by id), after {@code after} when given. */
    List<Message> findMessages(ConversationId conversationId, Optional<KeysetCursor> after, int limit);
}
