package com.example.ecommerce.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String phone; // 필수
    private String companyName;
    private String email;
    private String businessNumber;
    private String businessAddress;
    private String yardAddress;
}
