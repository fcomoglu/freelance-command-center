package com.freelancecommandcenter;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Mailpit SMTP server; tests read delivered e-mails through its HTTP API. */
public final class MailpitContainer extends GenericContainer<MailpitContainer> {

    private static final int SMTP_PORT = 1025;
    private static final int HTTP_PORT = 8025;

    public MailpitContainer() {
        super(DockerImageName.parse("axllent/mailpit:v1.34"));
        withExposedPorts(SMTP_PORT, HTTP_PORT);
    }

    public int smtpPort() {
        return getMappedPort(SMTP_PORT);
    }

    public String apiUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT) + "/api/v1";
    }
}
