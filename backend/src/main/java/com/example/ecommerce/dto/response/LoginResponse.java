package com.example.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class LoginResponse {
    private String username;
    private String role;
    private List<String> permissions;
    private String token; // 서비스에서 토큰까지 포함해서 반환
}
