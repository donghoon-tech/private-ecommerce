package com.example.ecommerce.config;

import com.example.ecommerce.util.PhoneEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

/**
 * JPA AttributeConverter: representative_phone 컬럼을 투명하게 AES-256 암호화/복호화.
 * <p>
 * - DB 쓰기(저장): 평문 → encrypt() → DB(암호문)
 * - DB 읽기(조회): DB(암호문) → decrypt() → 엔티티 필드(평문)
 * - autoApply = false: User 엔티티에서 명시적으로 @Convert 적용
 * </p>
 * <p>
 * ⚠️ WHERE 조건 검색 시 주의:
 * JPA가 파라미터를 자동으로 변환하지 않으므로,
 * Service에서 {@link PhoneEncryptor#encryptForSearch(String)}로 암호화한 값을
 * Repository에 전달해야 합니다.
 * </p>
 */
@Converter(autoApply = false)
@RequiredArgsConstructor
public class PhoneEncryptorConverter implements AttributeConverter<String, String> {

    private final PhoneEncryptor phoneEncryptor;

    @Override
    public String convertToDatabaseColumn(String plainPhone) {
        return phoneEncryptor.encrypt(plainPhone);
    }

    @Override
    public String convertToEntityAttribute(String encryptedPhone) {
        if (encryptedPhone == null) return null;
        return phoneEncryptor.decrypt(encryptedPhone);
    }
}
