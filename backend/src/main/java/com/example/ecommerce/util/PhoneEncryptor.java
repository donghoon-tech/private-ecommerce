package com.example.ecommerce.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 기반 핸드폰 번호 양방향 암호화 유틸리티.
 * <p>
 * - 암호화: 평문 → IV(12byte, 랜덤) + AES-256-GCM(태그 128bit) → Base64
 * - 복호화: Base64 → IV + 암호문 분리 → 복호화 → 평문
 * - 동일한 평문이어도 IV가 매번 달라져 저장값 다름(보안 강화)
 * - 단, DB WHERE 비교 시에는 반드시 {@link #encryptForSearch(String)}을 사용
 *   (고정 IV로 결정론적 암호화 → 검색용)
 * </p>
 */
@Component
public class PhoneEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /** 검색용 고정 IV (16진수 → 12바이트). 절대 변경하지 말 것. */
    private static final byte[] SEARCH_IV = new byte[]{
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
            0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C
    };

    private final SecretKeySpec secretKeySpec;

    /**
     * @param secretKey Base64로 인코딩된 32바이트 AES-256 키
     *                  (application-secret.properties: app.encryption.phone-secret-key)
     */
    public PhoneEncryptor(@Value("${app.encryption.phone-secret-key}") String secretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "app.encryption.phone-secret-key must be a Base64-encoded 32-byte (256-bit) key. " +
                    "Current decoded length: " + keyBytes.length + " bytes."
            );
        }
        this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 저장용 암호화 (랜덤 IV → 매번 다른 암호문).
     * DB 저장 시 사용. WHERE 검색에는 사용 불가.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = generateRandomIv();
            byte[] ciphertext = doEncrypt(plaintext.getBytes(), iv);
            // 저장 형식: Base64(IV + ciphertext)
            byte[] combined = combine(iv, ciphertext);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Phone encryption failed", e);
        }
    }

    /**
     * 검색용 결정론적 암호화 (고정 IV → 항상 동일한 암호문).
     * DB의 WHERE representative_phone = ? 비교 시 사용.
     */
    public String encryptForSearch(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] ciphertext = doEncrypt(plaintext.getBytes(), SEARCH_IV);
            byte[] combined = combine(SEARCH_IV, ciphertext);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Phone encryption (search) failed", e);
        }
    }

    /**
     * 복호화. SMS 발송 등 실제 번호가 필요한 경우 사용.
     * <p>
     * 기존 평문 데이터와의 호환성을 위해 다음의 경우 입력값을 그대로 반환합니다:
     * 1. Base64 형식이 아닌 경우
     * 2. 데이터 길이가 암호문(IV + Tag) 최소 길이보다 짧은 경우
     * 3. 복호화에 실패한 경우
     * </p>
     */
    public String decrypt(String val) {
        if (val == null || val.isEmpty()) return val;

        try {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(val);
            } catch (IllegalArgumentException e) {
                // Base64 형식이 아니면 평문으로 간주
                return val;
            }

            // GCM 암호문 최소 길이 체크: IV(12) + Tag(16) = 28 bytes
            // 이보다 짧으면 우리가 생성한 암호문이 아님
            if (decoded.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                return val;
            }

            byte[] iv = extractIv(decoded);
            byte[] ciphertext = extractCiphertext(decoded);
            byte[] plainBytes = doDecrypt(ciphertext, iv);
            return new String(plainBytes);
        } catch (Exception e) {
            // 복호화 실패 시(태그 불일치 등) 평문일 가능성을 고려하여 원본 반환
            return val;
        }
    }

    // ─────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────

    private byte[] doEncrypt(byte[] data, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec,
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(data);
    }

    private byte[] doDecrypt(byte[] ciphertext, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec,
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    private byte[] generateRandomIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private byte[] combine(byte[] iv, byte[] ciphertext) {
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return combined;
    }

    private byte[] extractIv(byte[] combined) {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
        return iv;
    }

    private byte[] extractCiphertext(byte[] combined) {
        int ciphertextLength = combined.length - GCM_IV_LENGTH_BYTES;
        byte[] ciphertext = new byte[ciphertextLength];
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertextLength);
        return ciphertext;
    }
}
