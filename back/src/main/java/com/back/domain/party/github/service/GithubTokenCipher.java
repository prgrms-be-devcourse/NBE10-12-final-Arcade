package com.back.domain.party.github.service;

import com.back.global.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** GitHub user access/refresh token을 DB에 저장하기 전 AES-GCM으로 암호화한다. */
@Component
public class GithubTokenCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    @Value("${custom.github.app.tokenEncryptionKey:}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] value = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, value, 0, iv.length);
            System.arraycopy(encrypted, 0, value, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(value);
        } catch (Exception e) {
            throw new ServiceException("500-20", "GITHUB_TOKEN_ENCRYPTION_FAILED");
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) return null;
        try {
            byte[] value = Base64.getDecoder().decode(encryptedText);
            if (value.length <= IV_LENGTH) throw new IllegalArgumentException();
            byte[] iv = java.util.Arrays.copyOfRange(value, 0, IV_LENGTH);
            byte[] cipherText = java.util.Arrays.copyOfRange(value, IV_LENGTH, value.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServiceException("500-20", "GITHUB_TOKEN_DECRYPTION_FAILED");
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptionKey);
            if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) throw new IllegalArgumentException();
            return new SecretKeySpec(decoded, "AES");
        } catch (Exception e) {
            throw new ServiceException("500-20", "GITHUB_TOKEN_ENCRYPTION_KEY_INVALID");
        }
    }
}
