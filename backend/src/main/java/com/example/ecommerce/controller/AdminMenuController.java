package com.example.ecommerce.controller;

import com.example.ecommerce.dto.MenuDTO;
import com.example.ecommerce.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('M_SYS_MENU:READ')")
    public ResponseEntity<List<MenuDTO>> getMenuTree() {
        return ResponseEntity.ok(menuService.getAllMenusAsTree());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('M_SYS_MENU:READ')")
    public ResponseEntity<List<MenuDTO>> getAllMenus() {
        return ResponseEntity.ok(menuService.getAllMenusFlat());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('M_SYS_MENU:CREATE')")
    public ResponseEntity<MenuDTO> createMenu(@RequestBody MenuDTO menuDTO) {
        return ResponseEntity.ok(menuService.createMenu(menuDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('M_SYS_MENU:UPDATE')")
    public ResponseEntity<MenuDTO> updateMenu(@PathVariable UUID id, @RequestBody MenuDTO menuDTO) {
        return ResponseEntity.ok(menuService.updateMenu(id, menuDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasAuthority('M_SYS_MENU:DELETE')")
    public ResponseEntity<Void> deleteMenu(@PathVariable UUID id) {
        menuService.deleteMenuRecursive(id);
        return ResponseEntity.noContent().build();
    }
}
