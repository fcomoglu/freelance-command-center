package com.freelancecommandcenter.messaging.application;

import com.freelancecommandcenter.messaging.api.MessageArrived;
import com.freelancecommandcenter.messaging.application.port.out.ConversationRepository;
import com.freelancecommandcenter.messaging.domain.Conversation;
import com.freelancecommandcenter.messaging.domain.Message;
import com.freelancecommandcenter.platform.api.MessageReceived;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.shared.id.Uuids;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds a received message to its conversation in the workspace's inbox, starting the conversation when needed. */
@Service
public class RecordMessageUseCase {

    private final ConversationRepository conversations;
    private final ApplicationEventPublisher events;

    RecordMessageUseCase(ConversationRepository conversations, ApplicationEventPublisher events) {
        this.conversations = conversations;
        this.events = events;
    }

    /** Idempotent: a message the conversation already has changes nothing and announces nothing. */
    @Transactional
    public void record(MessageReceived received) {
        PlatformMessage incoming = received.message();
        Message message = new Message(
                Uuids.v7(),
                incoming.externalId(),
                direction(incoming.direction()),
                detail(incoming.detail()),
                incoming.body(),
                incoming.sentAt());
        Conversation conversation = conversations.lockOrCreate(Conversation.start(
                received.workspaceId(),
                incoming.platform(),
                incoming.conversationId(),
                incoming.counterpart(),
                incoming.url(),
                message.detail(),
                message.sentAt()));
        if (!conversations.addMessage(conversation.id(), message)) {
            return;
        }
        conversation.record(message, incoming.counterpart(), incoming.url());
        conversations.save(conversation);
        if (message.direction() == Message.Direction.INBOUND) {
            events.publishEvent(new MessageArrived(
                    conversation.workspaceId(),
                    conversation.id(),
                    message.id(),
                    conversation.platform(),
                    incoming.counterpart(),
                    message.preview(),
                    incoming.url()));
        }
    }

    private static Message.Direction direction(PlatformMessage.Direction direction) {
        return switch (direction) {
            case INBOUND -> Message.Direction.INBOUND;
            case OUTBOUND -> Message.Direction.OUTBOUND;
        };
    }

    private static Message.Detail detail(PlatformMessage.Detail detail) {
        return switch (detail) {
            case FULL -> Message.Detail.FULL;
            case NOTICE -> Message.Detail.NOTICE;
        };
    }
}
