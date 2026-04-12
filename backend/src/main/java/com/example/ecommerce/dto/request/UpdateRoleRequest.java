package com.example.ecommerce.dto.request;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String role; // "admin" or "user"
}
