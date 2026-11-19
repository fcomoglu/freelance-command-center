package com.freelancecommandcenter.identity.adapter.in.web;

import com.freelancecommandcenter.identity.application.CurrentUserQuery;
import com.freelancecommandcenter.identity.application.CurrentUserQuery.CurrentUser;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account")
@RestController
class MeController {

    private final CurrentUserQuery currentUser;

    MeController(CurrentUserQuery currentUser) {
        this.currentUser = currentUser;
    }

    @Operation(summary = "The signed-in user", security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/me")
    MeResponse me(AuthenticatedUser caller) {
        CurrentUser user = currentUser.get(caller);
        return new MeResponse(user.userId(), user.workspaceId(), user.email(), user.emailVerified(), user.language());
    }

    @Schema(requiredProperties = {"userId", "workspaceId", "email", "emailVerified", "language"})
    record MeResponse(String userId, String workspaceId, String email, boolean emailVerified, String language) {}
}
