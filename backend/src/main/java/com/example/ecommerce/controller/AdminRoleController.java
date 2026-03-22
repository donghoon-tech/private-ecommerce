package com.example.ecommerce.controller;

import com.example.ecommerce.dto.RoleDTO;
import com.example.ecommerce.dto.RoleMenuActionRequest;
import com.example.ecommerce.service.RoleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('AUTH:ACCESS')")
public class AdminRoleController {

    private final RoleService roleService;



    /**
     * 특정 Role 조회
     */
    @GetMapping("/roles/{roleId}")
    public ResponseEntity<RoleDTO> getRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }

    /**
     * Role 생성
     */
    @PostMapping("/roles")
    public ResponseEntity<RoleDTO> createRole(@RequestBody RoleRequest request) {
        return ResponseEntity
                .ok(roleService.createRole(request.getName(), request.getDescription(), request.getMenuActions()));
    }

    /**
     * Role 수정 (이름/설명/Permission 바인딩)
     */
    @PutMapping("/roles/{roleId}")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable UUID roleId, @RequestBody RoleRequest request) {
        return ResponseEntity.ok(
                roleService.updateRole(roleId, request.getName(), request.getDescription(), request.getMenuActions()));
    }

    /**
     * Role 삭제 (기본 Role은 삭제 불가)
     */
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    @Data
    public static class RoleRequest {
        private String name;
        private String description;
        private List<RoleMenuActionRequest> menuActions;
    }
}
