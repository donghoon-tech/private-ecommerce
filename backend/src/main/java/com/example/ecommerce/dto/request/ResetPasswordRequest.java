package com.example.ecommerce.dto.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String username;
    private String phone;
    private String password;
}
