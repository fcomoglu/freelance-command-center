package com.freelancecommandcenter.identity.adapter.in.web;

import com.freelancecommandcenter.identity.application.CurrentUserQuery;
import com.freelancecommandcenter.identity.application.CurrentUserQuery.CurrentUser;
import com.freelancecommandcenter.identity.application.EmailVerificationUseCase;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account")
@RestController
class MeController {

    private final CurrentUserQuery currentUser;
    private final EmailVerificationUseCase emailVerification;

    MeController(CurrentUserQuery currentUser, EmailVerificationUseCase emailVerification) {
        this.currentUser = currentUser;
        this.emailVerification = emailVerification;
    }

    @Operation(summary = "The signed-in user", security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/me")
    MeResponse me(AuthenticatedUser caller) {
        CurrentUser user = currentUser.get(caller);
        return new MeResponse(user.userId(), user.workspaceId(), user.email(), user.emailVerified(), user.language());
    }

    @Operation(
            summary = "Send a new verification e-mail",
            description = "Earlier links stop working. Does nothing when the address is already verified.",
            security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/me/email-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendVerification(AuthenticatedUser caller) {
        emailVerification.sendVerification(caller.userId());
    }

    @Schema(requiredProperties = {"userId", "workspaceId", "email", "emailVerified", "language"})
    record MeResponse(String userId, String workspaceId, String email, boolean emailVerified, String language) {}
}
