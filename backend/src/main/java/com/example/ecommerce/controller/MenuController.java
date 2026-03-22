package com.example.ecommerce.controller;

import com.example.ecommerce.dto.MenuDTO;
import com.example.ecommerce.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/me")
    public ResponseEntity<List<MenuDTO>> getMyMenus(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(List.of());
        }

        // Extract ROLE name (e.g., ROLE_ADMIN -> ADMIN)
        String roleName = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.replace("ROLE_", ""))
                .findFirst()
                .orElse("USER");

        return ResponseEntity.ok(menuService.getMenusByRole(roleName));
    }
}
