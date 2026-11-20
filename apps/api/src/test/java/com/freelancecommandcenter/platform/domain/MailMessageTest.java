package com.freelancecommandcenter.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MailMessageTest {

    @Test
    void theExcerptIsThePlainTextOnOneLine() {
        assertThat(mail(Optional.of("  Your order\r\n\r\nwas  delivered.\n"), Optional.of("<p>ignored</p>"))
                        .excerpt(100))
                .isEqualTo("Your order was delivered.");
    }

    @Test
    void withoutPlainTextTheExcerptIsTheVisibleTextOfTheHtml() {
        String html = "<html><head><title>Fiverr</title><style>p { color: red; }</style></head><body>"
                + "<!-- preheader --><p>New&nbsp;order from <b>Grace</b> &amp; team</p>"
                + "<script>track()</script><p>&#8364;50 &#x2014; due &quot;Friday&quot;</p></body></html>";

        assertThat(mail(Optional.of(" "), Optional.of(html)).excerpt(100))
                .isEqualTo("New order from Grace & team €50 — due \"Friday\"");
    }

    @Test
    void inlineMarkupDoesNotSplitWordsOrSentences() {
        assertThat(mail(Optional.empty(), Optional.of("<div>Logo <b>design</b>.</div><div>Su<span>m</span>mary</div>"))
                        .excerpt(100))
                .isEqualTo("Logo design. Summary");
    }

    @Test
    void aLongTextIsCutAtAWord() {
        assertThat(mail(Optional.of("one two three four five"), Optional.empty())
                        .excerpt(12))
                .isEqualTo("one two…");
    }

    @Test
    void aMailWithoutTextHasAnEmptyExcerpt() {
        assertThat(mail(Optional.empty(), Optional.empty()).excerpt(100)).isEmpty();
    }

    private static MailMessage mail(Optional<String> text, Optional<String> html) {
        return new MailMessage(Optional.empty(), "orders@fiverr.com", "Order", Optional.empty(), text, html);
    }
}
