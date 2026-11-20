package com.freelancecommandcenter.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import org.junit.jupiter.api.Test;

class MailInboxTest {

    @Test
    void newInboxesGetDistinctRandomAddresses() {
        WorkspaceId workspace = WorkspaceId.newId();

        MailInbox first = MailInbox.newFor(workspace);
        MailInbox second = MailInbox.newFor(workspace);

        assertThat(first.localPart()).matches("[a-z2-7]{20}").isNotEqualTo(second.localPart());
        assertThat(first.address("in.example.com")).isEqualTo(first.localPart() + "@in.example.com");
    }

    @Test
    void theLocalPartIsReadIgnoringCaseAndPlusTags() {
        assertThat(MailInbox.localPartOf(" AbC+bionluk@In.Example.com ", "in.example.com"))
                .contains("abc");
        assertThat(MailInbox.localPartOf("abc@other.example.com", "in.example.com"))
                .isEmpty();
        assertThat(MailInbox.localPartOf("@in.example.com", "in.example.com")).isEmpty();
        assertThat(MailInbox.localPartOf("no-at-sign", "in.example.com")).isEmpty();
    }

    @Test
    void theSenderDomainNamesThePlatform() {
        assertThat(MailSenders.platformOf("noreply@bionluk.com")).contains(PlatformCode.BIONLUK);
        assertThat(MailSenders.platformOf("Briefs@e.FIVERR.com")).contains(PlatformCode.FIVERR);
        assertThat(MailSenders.platformOf("team@arc.dev")).contains(PlatformCode.ARC_DEV);
        assertThat(MailSenders.platformOf("someone@notfiverr.com")).isEmpty();
        assertThat(MailSenders.platformOf("")).isEmpty();
    }
}
