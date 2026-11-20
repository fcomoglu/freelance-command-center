package com.freelancecommandcenter.platform.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY =
            Base64.getEncoder().encodeToString("x".repeat(32).getBytes(StandardCharsets.UTF_8));

    private final TokenCipher cipher = new TokenCipher(KEY);

    @Test
    void decryptsWhatItEncryptedWithoutStoringThePlainText() {
        byte[] sealed = cipher.encrypt("secret-token");

        assertThat(new String(sealed, StandardCharsets.ISO_8859_1)).doesNotContain("secret-token");
        assertThat(cipher.decrypt(sealed)).isEqualTo("secret-token");
    }

    @Test
    void encryptsTheSameValueDifferentlyEachTime() {
        assertThat(cipher.encrypt("secret-token")).isNotEqualTo(cipher.encrypt("secret-token"));
    }

    @Test
    void refusesValuesFromAnotherKeyOrAltered() {
        byte[] sealed = cipher.encrypt("secret-token");

        assertThatThrownBy(() -> new TokenCipher(OTHER_KEY).decrypt(sealed)).isInstanceOf(IllegalStateException.class);
        sealed[sealed.length - 1] ^= 1;
        assertThatThrownBy(() -> cipher.decrypt(sealed)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsOnlyA256BitKey() {
        assertThatThrownBy(() -> new TokenCipher("c2hvcnQ=")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenCipher("not base64!")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new TokenCipher("").configured()).isFalse();
        assertThatThrownBy(() -> new TokenCipher("").encrypt("x")).isInstanceOf(IllegalStateException.class);
    }
}
