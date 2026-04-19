package com.example.ecommerce.controller;

import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.dto.request.*;
import java.util.Map;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
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
            LoginResponse loginResponse = authService.login(loginRequest);

            ResponseCookie cookie = ResponseCookie.from("jwt", loginResponse.getToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(loginResponse);
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

    @PostMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestBody CheckUsernameRequest request) {
        boolean exists = authService.checkUsernameExists(request.getUsername());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getUsername(), request.getPhone(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 재설정되었습니다."));
    }
}