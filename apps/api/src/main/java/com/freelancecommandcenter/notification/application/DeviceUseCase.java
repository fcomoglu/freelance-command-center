package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.notification.application.port.out.DeviceRepository;
import com.freelancecommandcenter.notification.domain.Device;
import com.freelancecommandcenter.shared.i18n.Language;
import com.freelancecommandcenter.shared.id.Uuids;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers the caller's phone for push notifications and removes it again on sign-out. */
@Service
public class DeviceUseCase {

    private final DeviceRepository devices;

    DeviceUseCase(DeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional
    public Device register(AuthenticatedUser caller, String pushToken, Language language) {
        return devices.register(new Device(Uuids.v7(), caller.userId(), caller.workspaceId(), pushToken, language));
    }

    @Transactional
    public void unregister(AuthenticatedUser caller, UUID deviceId) {
        devices.unregister(deviceId, caller.userId());
    }
}
