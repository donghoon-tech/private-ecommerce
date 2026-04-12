package com.example.ecommerce.util;

import com.example.ecommerce.exception.BusinessException;

public class ValidationUtils {
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$";

    public static void validatePassword(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new BusinessException("비밀번호는 영문과 숫자를 포함하여 최소 8자 이상이어야 합니다.");
        }
    }
}
