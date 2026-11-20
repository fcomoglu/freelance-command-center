package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.matching.api.JobMatched;
import com.freelancecommandcenter.notification.application.port.out.DeviceRepository;
import com.freelancecommandcenter.notification.application.port.out.JobNotificationLog;
import com.freelancecommandcenter.notification.application.port.out.PushSender;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushMessage;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushUnavailableException;
import com.freelancecommandcenter.notification.domain.Device;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pushes a matched job to the phones of the workspace, once per workspace and job. Push is best effort: when the push
 * service fails, the notification is logged and not retried, because a late "new job" alert has little value.
 */
@Service
public class NotifyJobMatchUseCase {

    /** Android notification channel of new-job alerts; the app creates it with the same id. */
    public static final String JOBS_CHANNEL = "jobs";

    private static final Logger log = LoggerFactory.getLogger(NotifyJobMatchUseCase.class);

    private final JobNotificationLog notified;
    private final DeviceRepository devices;
    private final PushSender push;
    private final MessageSource messages;
    private final Clock clock;

    NotifyJobMatchUseCase(
            JobNotificationLog notified,
            DeviceRepository devices,
            PushSender push,
            MessageSource messages,
            Clock clock) {
        this.notified = notified;
        this.devices = devices;
        this.push = push;
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional
    public void notify(JobMatched match) {
        if (!notified.recordFirst(match.workspaceId(), match.jobId(), clock.instant())) {
            return;
        }
        List<Device> targets = devices.findByWorkspace(match.workspaceId());
        if (targets.isEmpty()) {
            return;
        }
        List<PushMessage> pushes =
                targets.stream().map(device -> message(device, match)).toList();
        try {
            List<String> gone = push.send(pushes);
            if (!gone.isEmpty()) {
                devices.deleteByPushTokens(gone);
            }
        } catch (PushUnavailableException exception) {
            log.warn(
                    "Push for job {} to workspace {} failed: {}",
                    match.jobId(),
                    match.workspaceId(),
                    exception.getMessage());
        }
    }

    private PushMessage message(Device device, JobMatched match) {
        String platform = messages.getMessage(
                "platform." + match.platform().value(), null, device.language().locale());
        String title = messages.getMessage(
                "notification.push.job-matched.title",
                new Object[] {platform},
                device.language().locale());
        return new PushMessage(
                device.pushToken(),
                title,
                match.title(),
                JOBS_CHANNEL,
                Map.of(
                        "type", "job",
                        "jobId", match.jobId().toString(),
                        "url", match.url().toString()));
    }
}
