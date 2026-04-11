package com.example.ecommerce.controller;

import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.security.JwtTokenProvider;
import com.example.ecommerce.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "로그아웃 되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(), loginRequest.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            String username = authentication.getName();

            // ROLE_ 로 시작하는 권한(역할) 찾기
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .findFirst()
                    .map(auth -> auth.replace("ROLE_", ""))
                    .orElse("USER");

            // 역할(Role)을 제외한 나머지 모든 실제 액션 퍼미션 수집
            List<String> permissions = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> !auth.startsWith("ROLE_"))
                    .collect(Collectors.toList());

            String token = jwtTokenProvider.createToken(username, role);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("username", username);
            responseBody.put("role", role);
            responseBody.put("permissions", permissions);

            ResponseCookie cookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(false) // HTTPS에서는 true
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(responseBody);
        } catch (BadCredentialsException e) {
            log.error("Login failed: Invalid credentials for user {}", loginRequest.getUsername());
            return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
        } catch (AuthenticationException e) {
            log.error("Login failed: Authentication error for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", "로그인에 실패했습니다."));
        } catch (Exception e) {
            log.error("Login failed: Unexpected error for user {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("message", "서버 내부 오류: " + e.getMessage()));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/find-id")
    public ResponseEntity<Map<String, String>> findId(@RequestBody FindIdRequest request) {
        String username = authService.findId(request.getPhone());
        return ResponseEntity.ok(Map.of("username", username));
    }

    @PostMapping("/check-phone")
    public ResponseEntity<Map<String, Boolean>> checkPhone(@RequestBody CheckPhoneRequest request) {
        boolean exists = authService.checkPhoneExists(request.getPhone());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/check-user-phone")
    public ResponseEntity<Map<String, Boolean>> checkUserAndPhone(@RequestBody CheckUserPhoneRequest request) {
        boolean exists = authService.checkUserAndPhoneExists(request.getUsername(), request.getPhone());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        String tempPassword = authService.resetPassword(request.getUsername(), request.getPhone());
        return ResponseEntity.ok(Map.of("tempPassword", tempPassword));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String phone; // 필수
        private String companyName;
        private String email;
        private String businessNumber;
        private String businessAddress;
        private String yardAddress;
    }

    @Data
    public static class FindIdRequest {
        private String phone;
    }

    @Data
    public static class CheckPhoneRequest {
        private String phone;
    }

    @Data
    public static class CheckUserPhoneRequest {
        private String username;
        private String phone;
    }

    @Data
    public static class ResetPasswordRequest {
        private String username;
        private String phone;
    }
}