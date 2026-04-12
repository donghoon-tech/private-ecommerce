package com.example.ecommerce.util;

import com.example.ecommerce.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.ecommerce.constant.ErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilsTest {

    @Test
    @DisplayName("비밀번호가 영문과 숫자를 포함하여 8자 이상이면 검증을 통과한다")
    void validatePassword_success() {
        assertThatCode(() -> ValidationUtils.validatePassword("password123"))
                .doesNotThrowAnyException();
        
        assertThatCode(() -> ValidationUtils.validatePassword("test@2024"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호에 영문이 없으면 예외가 발생한다")
    void validatePassword_noLetter_fail() {
        assertThatThrownBy(() -> ValidationUtils.validatePassword("12345678"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorMessage.PASSWORD_INVALID);
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 예외가 발생한다")
    void validatePassword_noDigit_fail() {
        assertThatThrownBy(() -> ValidationUtils.validatePassword("password@"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorMessage.PASSWORD_INVALID);
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 예외가 발생한다")
    void validatePassword_tooShort_fail() {
        assertThatThrownBy(() -> ValidationUtils.validatePassword("p12345"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorMessage.PASSWORD_INVALID);
    }

    @Test
    @DisplayName("전화번호에서 숫자를 제외한 문자를 제거한다")
    void normalizePhone_success() {
        assertThat(ValidationUtils.normalizePhone("010-1234-5678")).isEqualTo("01012345678");
        assertThat(ValidationUtils.normalizePhone("02) 123-4567")).isEqualTo("021234567");
        assertThat(ValidationUtils.normalizePhone(null)).isNull();
    }
}
