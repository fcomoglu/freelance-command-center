package com.freelancecommandcenter.platform.adapter.out.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts platform tokens at rest with AES-256-GCM, so a database dump alone does not give access to anyone's
 * platform account. Each value is stored as its random 96-bit nonce followed by the ciphertext and tag.
 */
final class TokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    /** @param base64Key empty when no platform's OAuth app is configured; then nothing can be encrypted */
    TokenCipher(String base64Key) {
        if (base64Key.isBlank()) {
            this.key = null;
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.strip());
        } catch (IllegalArgumentException exception) {
            throw invalidKey();
        }
        if (bytes.length != KEY_BYTES) {
            throw invalidKey();
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    boolean configured() {
        return key != null;
    }

    byte[] encrypt(String plain) {
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(NONCE_BYTES + sealed.length)
                    .put(nonce)
                    .put(sealed)
                    .array();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Token encryption failed", exception);
        }
    }

    /** @throws IllegalStateException when the value was encrypted with another key or altered */
    String decrypt(byte[] stored) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, stored, 0, NONCE_BYTES));
            byte[] plain = cipher.doFinal(stored, NONCE_BYTES, stored.length - NONCE_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Token decryption failed; was the token key changed?", exception);
        }
    }

    private SecretKey requireKey() {
        if (key == null) {
            throw new IllegalStateException("No token key configured (fcc.platform.connections.token-key)");
        }
        return key;
    }

    private static IllegalArgumentException invalidKey() {
        return new IllegalArgumentException(
                "fcc.platform.connections.token-key must be " + KEY_BYTES + " random bytes in base64");
    }
}
