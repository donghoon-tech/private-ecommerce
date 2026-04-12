package com.example.ecommerce.dto.request;

import lombok.Data;

@Data
public class CheckUserPhoneRequest {
    private String username;
    private String phone;
}
