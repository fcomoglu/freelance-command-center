package com.freelancecommandcenter.shared.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidsTest {

    @Test
    void generatesVersion7VariantRfcIdentifiers() {
        UUID id = Uuids.v7();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void identifiersFromLaterMillisecondsSortAfterEarlierOnes() throws InterruptedException {
        UUID earlier = Uuids.v7();
        Thread.sleep(2);
        UUID later = Uuids.v7();

        assertThat(later.toString()).isGreaterThan(earlier.toString());
    }
}
