package com.freelancecommandcenter.platform.adapter.in.web;

import com.freelancecommandcenter.platform.application.PlatformProperties;
import com.freelancecommandcenter.platform.application.ReceiveMailUseCase;
import com.freelancecommandcenter.platform.application.port.out.MailReader.UnreadableMailException;
import io.swagger.v3.oas.annotations.Hidden;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives raw MIME messages from the mail routing service of the inbound domain (ADR 0012). Not part of the client
 * API: it authenticates the routing service with a shared secret instead of a user token.
 */
@Hidden
@RestController
class InboundMailWebhook {

    static final String PATH = "/api/v1/inbound-mail";
    static final String RECIPIENT_HEADER = "X-Envelope-To";

    private final ReceiveMailUseCase receiver;
    private final PlatformProperties.Mail settings;

    InboundMailWebhook(ReceiveMailUseCase receiver, PlatformProperties properties) {
        this.receiver = receiver;
        this.settings = properties.mail();
    }

    /**
     * 202 for every mail from an authenticated sender, also for unknown recipients, so the routing service never
     * retries a mail that cannot be delivered.
     */
    @PostMapping(PATH)
    ResponseEntity<Void> receive(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(name = RECIPIENT_HEADER, required = false) String recipient,
            @RequestBody byte[] raw) {
        if (!authorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (raw.length > settings.maxSize().toBytes()) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        }
        try {
            receiver.receive(recipient, raw);
        } catch (UnreadableMailException exception) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.accepted().build();
    }

    private boolean authorized(String authorization) {
        String secret = settings.webhookSecret();
        if (secret.isBlank() || authorization == null) {
            return false;
        }
        return MessageDigest.isEqual(
                authorization.getBytes(StandardCharsets.UTF_8), ("Bearer " + secret).getBytes(StandardCharsets.UTF_8));
    }
}
