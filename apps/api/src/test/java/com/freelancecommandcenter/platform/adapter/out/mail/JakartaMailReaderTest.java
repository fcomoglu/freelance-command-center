package com.freelancecommandcenter.platform.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.platform.application.port.out.MailReader.UnreadableMailException;
import com.freelancecommandcenter.platform.domain.MailMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JakartaMailReaderTest {

    private final JakartaMailReader reader = new JakartaMailReader();

    @Test
    void readsTheHeadersAndTheBodiesOutsideAttachments() {
        MailMessage mail = reader.read(bytes("""
                From: Arc <jobs@arc.dev>
                Subject: =?UTF-8?B?WWVuaSBpxZ8=?=
                Date: Fri, 20 Nov 2026 12:00:00 +0300
                Message-ID: <1@arc.dev>
                MIME-Version: 1.0
                Content-Type: multipart/mixed; boundary="outer"

                --outer
                Content-Type: text/plain; charset=UTF-8
                Content-Disposition: attachment; filename="notes.txt"

                Not the body
                --outer
                Content-Type: multipart/alternative; boundary="inner"

                --inner
                Content-Type: text/plain; charset=UTF-8
                Content-Transfer-Encoding: quoted-printable

                Uzaktan =C3=A7al=C4=B1=C5=9Fma
                --inner
                Content-Type: text/html; charset=UTF-8

                <p>Remote</p>
                --inner--
                --outer--
                """));

        assertThat(mail.messageId()).contains("<1@arc.dev>");
        assertThat(mail.sender()).isEqualTo("jobs@arc.dev");
        assertThat(mail.subject()).isEqualTo("Yeni iş");
        assertThat(mail.sentAt()).contains(Instant.parse("2026-11-20T09:00:00Z"));
        assertThat(mail.text())
                .hasValueSatisfying(text -> assertThat(text.strip()).isEqualTo("Uzaktan çalışma"));
        assertThat(mail.html())
                .hasValueSatisfying(html -> assertThat(html.strip()).isEqualTo("<p>Remote</p>"));
    }

    @Test
    void aMailWithoutOptionalHeadersOrABodyIsStillRead() {
        MailMessage mail = reader.read(bytes("Subject: Hello\n\n"));

        assertThat(mail.messageId()).isEmpty();
        assertThat(mail.sender()).isEmpty();
        assertThat(mail.sentAt()).isEmpty();
        assertThat(mail.html()).isEmpty();
    }

    @Test
    void headersInRawUtf8AreRead() {
        MailMessage mail =
                reader.read(bytes("From: Bionluk <noreply@bionluk.com>\nSubject: Yeni bir değerlendirme aldın\n\n"));

        assertThat(mail.subject()).isEqualTo("Yeni bir değerlendirme aldın");
    }

    @Test
    void aBrokenHeaderIsRejected() {
        assertThatThrownBy(() -> reader.read(bytes("From: <<<\nSubject: x\n\nBody\n")))
                .isInstanceOf(UnreadableMailException.class);
    }

    private static byte[] bytes(String mail) {
        return mail.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
    }
}
