package com.example.ecommerce.controller;

import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    /**
     * 전체 사용자 목록 조회 (BusinessProfile 포함)
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersWithProfiles());
    }

    /**
     * 사용자 역할 변경 (user <-> admin)
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.updateUserRole(userId, request.getRole()));
    }

    /**
     * 사업자 프로필 승인
     */
    @PutMapping("/business-profiles/{profileId}/approve")
    public ResponseEntity<Map<String, String>> approveBusinessProfile(
            @PathVariable UUID profileId,
            @RequestHeader("Authorization") String token) {
        userService.approveBusinessProfile(profileId, token);
        return ResponseEntity.ok(Map.of("message", "승인되었습니다."));
    }

    /**
     * 사업자 프로필 반려
     */
    @PutMapping("/business-profiles/{profileId}/reject")
    public ResponseEntity<Map<String, String>> rejectBusinessProfile(
            @PathVariable UUID profileId,
            @RequestBody RejectRequest request) {
        userService.rejectBusinessProfile(profileId, request.getReason());
        return ResponseEntity.ok(Map.of("message", "반려되었습니다."));
    }

    @Data
    public static class UpdateRoleRequest {
        private String role; // "admin" or "user"
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }
}
