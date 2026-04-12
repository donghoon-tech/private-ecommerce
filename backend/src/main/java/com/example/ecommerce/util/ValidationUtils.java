package com.example.ecommerce.util;

import com.example.ecommerce.constant.ErrorMessage;
import com.example.ecommerce.exception.BusinessException;

public class ValidationUtils {
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$";

    public static void validatePassword(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new BusinessException(ErrorMessage.PASSWORD_INVALID);
        }
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.replaceAll("[^0-9]", "");
    }
}
