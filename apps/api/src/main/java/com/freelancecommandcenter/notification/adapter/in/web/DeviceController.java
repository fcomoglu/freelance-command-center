package com.freelancecommandcenter.notification.adapter.in.web;

import com.freelancecommandcenter.notification.application.DeviceUseCase;
import com.freelancecommandcenter.notification.domain.Device;
import com.freelancecommandcenter.shared.i18n.Language;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Devices")
@RestController
class DeviceController {

    private final DeviceUseCase devices;

    DeviceController(DeviceUseCase devices) {
        this.devices = devices;
    }

    @Operation(
            summary = "Register this phone for push notifications",
            description = "Registering a known push token again moves it to the caller and returns the same id.",
            security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/devices")
    DeviceResponse register(AuthenticatedUser caller, @Valid @RequestBody DeviceRequest request) {
        Device device = devices.register(caller, request.pushToken(), Language.fromTag(request.language()));
        return new DeviceResponse(device.id().toString());
    }

    @Operation(
            summary = "Stop push notifications to a phone",
            description = "Does nothing when the device is unknown or belongs to someone else.",
            security = @SecurityRequirement(name = "bearer"))
    @DeleteMapping("/api/v1/devices/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unregister(AuthenticatedUser caller, @PathVariable UUID deviceId) {
        devices.unregister(caller, deviceId);
    }

    @Schema(
            name = "DeviceRequest",
            requiredProperties = {"pushToken", "language"})
    record DeviceRequest(
            @Schema(description = "Expo push token", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
            @NotBlank
            @Size(max = 255)
            String pushToken,

            @Schema(
                    description = "Language of the notification texts",
                    allowableValues = {"en", "tr"})
            @NotBlank
            @Size(max = 35)
            String language) {}

    @Schema(
            name = "Device",
            requiredProperties = {"id"})
    record DeviceResponse(String id) {}
}
