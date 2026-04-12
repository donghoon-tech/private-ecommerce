package com.example.ecommerce.controller;

import com.example.ecommerce.dto.MenuDTO;
import com.example.ecommerce.service.MenuService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuService menuService;

    /**
     * 관리자: 전체 원본 메뉴 트리 조회
     */
    @GetMapping("/admin/menus/tree")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('PG_SYS_AUTH')")
    public ResponseEntity<List<MenuDTO>> getAllMenus() {
        return ResponseEntity.ok(menuService.getAllMenuTree());
    }

    /**
     * 로그인한 사용자: 자신에게 허용된 메뉴 트리만 조회
     */
    @GetMapping("/menus/me")
    public ResponseEntity<List<MenuDTO>> getMyMenus(Principal principal) {
        String username = (principal != null) ? principal.getName() : null;
        return ResponseEntity.ok(menuService.getUserMenuTree(username));
    }

    @PostMapping("/admin/menus")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('PG_SYS_AUTH')")
    public ResponseEntity<MenuDTO> createMenu(@RequestBody MenuRequest request) {
        return ResponseEntity.ok(menuService.createMenu(
                request.getParentId(),
                request.getProgramId(),
                request.getName(),
                request.getSortOrder(),
                request.getIsVisible(),
                request.getPath()
        ));
    }

    @PutMapping("/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('PG_SYS_AUTH')")
    public ResponseEntity<MenuDTO> updateMenu(@PathVariable UUID id, @RequestBody MenuRequest request) {
        return ResponseEntity.ok(menuService.updateMenu(
                id,
                request.getParentId(),
                request.getProgramId(),
                request.getName(),
                request.getSortOrder(),
                request.getIsVisible(),
                request.getPath()
        ));
    }

    @DeleteMapping("/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('PG_SYS_AUTH')")
    public ResponseEntity<Map<String, String>> deleteMenu(@PathVariable UUID id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    @Data
    public static class MenuRequest {
        private UUID parentId;
        private UUID programId;
        private String name;
        private Integer sortOrder;
        private Boolean isVisible;
        private String path;
    }
}
