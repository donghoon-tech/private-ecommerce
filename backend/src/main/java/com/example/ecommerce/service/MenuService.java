package com.example.ecommerce.service;

import com.example.ecommerce.dto.MenuDTO;
import com.example.ecommerce.entity.Menu;
import com.example.ecommerce.repository.MenuRepository;
import com.example.ecommerce.repository.RoleMenuActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final RoleMenuActionRepository roleMenuActionRepository;

    public List<MenuDTO> getAllMenusAsTree() {
        List<Menu> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
        return buildTree(allMenus);
    }

    public List<MenuDTO> getAllMenusFlat() {
        return menuRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MenuDTO createMenu(MenuDTO dto) {
        Menu menu = Menu.builder()
                .menuCode(dto.getMenuCode())
                .name(dto.getName())
                .parentId(dto.getParentId())
                .sortOrder(dto.getSortOrder())
                .isVisible(dto.isVisible())
                .build();
        return toDTO(menuRepository.save(menu));
    }

    @Transactional
    public MenuDTO updateMenu(UUID id, MenuDTO dto) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu not found"));
        
        menu.setMenuCode(dto.getMenuCode());
        menu.setName(dto.getName());
        menu.setSortOrder(dto.getSortOrder());
        menu.setVisible(dto.isVisible());
        // Note: Changing parentId could cause circular references if not handled carefully.
        // For simplicity, we assume frontend provides a valid parentId.
        menu.setParentId(dto.getParentId());

        return toDTO(menuRepository.save(menu));
    }

    @Transactional
    public void deleteMenuRecursive(UUID id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu not found"));
        
        // Find and delete all children recursively
        List<Menu> children = menuRepository.findByParentId(id);
        for (Menu child : children) {
            deleteMenuRecursive(child.getId());
        }

        // Remove role association to avoid FK constraint violations
        roleMenuActionRepository.deleteByMenuId(id);
        
        // Finally, delete the menu itself
        menuRepository.delete(menu);
    }

    public List<MenuDTO> getMenusByRole(String roleName) {
        // If ADMIN or DEVELOPER, show all visible menus
        if ("ADMIN".equals(roleName) || "DEVELOPER".equals(roleName)) {
            return getAllMenusAsTree().stream()
                    .filter(MenuDTO::isVisible)
                    .collect(Collectors.toList());
        }

        // For other roles, find associated RoleMenuActions
        List<Menu> menus = menuRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(m -> m.isVisible() && roleMenuActionRepository.existsByRole_NameAndMenu_IdAndCanReadTrue(roleName, m.getId()))
                .collect(Collectors.toList());
        
        return buildTree(menus);
    }

    private MenuDTO toDTO(Menu menu) {
        return MenuDTO.builder()
                .id(menu.getId())
                .menuCode(menu.getMenuCode())
                .name(menu.getName())
                .parentId(menu.getParentId())
                .sortOrder(menu.getSortOrder())
                .isVisible(menu.isVisible())
                .build();
    }

    private List<MenuDTO> buildTree(List<Menu> menus) {
        List<MenuDTO> dtos = menus.stream().map(this::toDTO).collect(Collectors.toList());
        Map<UUID, List<MenuDTO>> mapByParentId = dtos.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(MenuDTO::getParentId));

        List<MenuDTO> rootMenus = new ArrayList<>();
        for (MenuDTO dto : dtos) {
            dto.setChildren(mapByParentId.getOrDefault(dto.getId(), new ArrayList<>()));
            if (dto.getParentId() == null) {
                rootMenus.add(dto);
            }
        }
        return rootMenus;
    }
}
