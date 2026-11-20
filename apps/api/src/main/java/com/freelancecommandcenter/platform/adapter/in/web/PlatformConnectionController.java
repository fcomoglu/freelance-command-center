package com.freelancecommandcenter.platform.adapter.in.web;

import com.freelancecommandcenter.platform.application.ConnectAccountUseCase;
import com.freelancecommandcenter.platform.application.PlatformConnectionsUseCase;
import com.freelancecommandcenter.platform.application.PlatformConnectionsUseCase.ConnectablePlatform;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import com.freelancecommandcenter.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Platform connections")
@RestController
class PlatformConnectionController {

    private final PlatformConnectionsUseCase connections;
    private final ConnectAccountUseCase connecting;

    PlatformConnectionController(PlatformConnectionsUseCase connections, ConnectAccountUseCase connecting) {
        this.connections = connections;
        this.connecting = connecting;
    }

    @Operation(
            summary = "The platforms whose accounts can be connected, with the workspace's connections",
            description = "Only platforms whose OAuth app is configured on the server are listed; there is one page.",
            security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/platform-connections")
    PageResponse<PlatformConnectionResponse> list(AuthenticatedUser caller) {
        return new PageResponse<>(
                connections.list(caller.workspaceId()).stream()
                        .map(PlatformConnectionController::toResponse)
                        .toList(),
                null);
    }

    @Operation(
            summary = "Start connecting an account",
            description = "Send the user to `authorizationUrl`. The platform returns them to the web client at"
                    + " `/connections/{platform}/callback` with `code` and `state`, which complete the connection.",
            security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/platform-connections/{platform}/authorizations")
    AuthorizationResponse authorize(AuthenticatedUser caller, @PathVariable PlatformCode platform) {
        return new AuthorizationResponse(
                connecting.start(caller.workspaceId(), platform).toString());
    }

    @Operation(
            summary = "Complete connecting an account",
            description = "Replaces the workspace's previous connection to the platform.",
            security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/platform-connections/{platform}")
    PlatformConnectionResponse connect(
            AuthenticatedUser caller, @PathVariable PlatformCode platform, @Valid @RequestBody ConnectRequest request) {
        PlatformConnection connection =
                connecting.complete(caller.workspaceId(), platform, request.code(), request.state());
        return toResponse(new ConnectablePlatform(platform, Optional.of(connection)));
    }

    @Operation(
            summary = "Disconnect an account",
            description = "Its tokens are deleted; conversations read so far stay in the inbox.",
            security = @SecurityRequirement(name = "bearer"))
    @DeleteMapping("/api/v1/platform-connections/{platform}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disconnect(AuthenticatedUser caller, @PathVariable PlatformCode platform) {
        connections.disconnect(caller.workspaceId(), platform);
    }

    private static PlatformConnectionResponse toResponse(ConnectablePlatform item) {
        return item.connection()
                .map(connection -> new PlatformConnectionResponse(
                        item.platform(),
                        connection.status().name().toLowerCase(Locale.ROOT),
                        connection.accountName(),
                        connection.connectedAt()))
                .orElseGet(() -> new PlatformConnectionResponse(item.platform(), "not_connected", null, null));
    }

    @Schema(
            name = "PlatformConnection",
            requiredProperties = {"platform", "status"})
    record PlatformConnectionResponse(
            PlatformCode platform,

            @Schema(
                    description = "`reconnect_required`: the platform no longer accepts the authorization",
                    allowableValues = {"not_connected", "active", "reconnect_required"})
            String status,

            @Schema(description = "The account's name on the platform; absent when not connected")
            String accountName,

            Instant connectedAt) {}

    @Schema(
            name = "PlatformAuthorization",
            requiredProperties = {"authorizationUrl"})
    record AuthorizationResponse(
            @Schema(description = "The platform's consent page")
            String authorizationUrl) {}

    @Schema(name = "ConnectPlatformRequest")
    record ConnectRequest(
            @Schema(description = "`code` from the callback") @NotBlank
            String code,

            @Schema(description = "`state` from the callback") @NotBlank
            String state) {}
}
