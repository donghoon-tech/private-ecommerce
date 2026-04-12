package com.example.ecommerce.util;

import com.example.ecommerce.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                .hasMessage("비밀번호는 영문과 숫자를 포함하여 최소 8자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 예외가 발생한다")
    void validatePassword_noDigit_fail() {
        assertThatThrownBy(() -> ValidationUtils.validatePassword("password@"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("비밀번호는 영문과 숫자를 포함하여 최소 8자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 예외가 발생한다")
    void validatePassword_tooShort_fail() {
        assertThatThrownBy(() -> ValidationUtils.validatePassword("p12345"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("비밀번호는 영문과 숫자를 포함하여 최소 8자 이상이어야 합니다.");
    }
}
