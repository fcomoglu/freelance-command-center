package com.freelancecommandcenter.notification.domain;

import com.freelancecommandcenter.shared.i18n.Language;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A phone that receives push notifications for the workspace of the user signed in on it.
 *
 * @param pushToken Expo push token of the app installation; one device per token
 * @param language language of the notification texts, the app's language on that phone
 */
public record Device(UUID id, UserId userId, WorkspaceId workspaceId, String pushToken, Language language) {

    private static final Pattern EXPO_PUSH_TOKEN = Pattern.compile("^Expo(nent)?PushToken\\[[^\\]]{1,200}]$");

    public Device {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(language, "language");
        if (pushToken == null || !EXPO_PUSH_TOKEN.matcher(pushToken).matches()) {
            throw new InvalidPushTokenException();
        }
    }
}
