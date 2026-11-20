package com.freelancecommandcenter.notification.application.port.out;

import com.freelancecommandcenter.notification.domain.Device;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DeviceRepository {

    /**
     * Stores the device under its push token. A token that is already registered moves to the given user, since the
     * phone now belongs to whoever signed in last; the stored device keeps its id.
     *
     * @return the stored device
     */
    Device register(Device device);

    /** Removes the device when it belongs to {@code userId}; does nothing otherwise. */
    void unregister(UUID deviceId, UserId userId);

    List<Device> findByWorkspace(WorkspaceId workspaceId);

    void deleteByPushTokens(Collection<String> pushTokens);
}
