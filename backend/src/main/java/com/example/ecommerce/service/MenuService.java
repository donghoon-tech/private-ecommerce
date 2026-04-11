package com.example.ecommerce.service;

import com.example.ecommerce.dto.MenuDTO;
import com.example.ecommerce.entity.Menu;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.MenuRepository;
import com.example.ecommerce.repository.ProgramRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final MenuRepository menuRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MenuDTO> getAllMenuTree() {
        return menuRepository.findByParentIsNullOrderBySortOrderAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getUserMenuTree(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Set<UUID> userProgramIds = user.getRole().getPrograms().stream()
                .map(Program::getId)
                .collect(Collectors.toSet());

        List<Menu> roots = menuRepository.findByParentIsNullOrderBySortOrderAsc();
        List<MenuDTO> result = new ArrayList<>();
        
        for (Menu root : roots) {
            if (!root.getIsVisible()) continue;
            MenuDTO dto = filterMenuTree(root, userProgramIds);
            if (dto != null) {
                result.add(dto);
            }
        }
        return result;
    }

    private MenuDTO filterMenuTree(Menu menu, Set<UUID> userProgramIds) {
        // If it's a leaf node with a program 
        if (menu.getProgram() != null) {
            if (userProgramIds.contains(menu.getProgram().getId())) {
                return toDTO(menu);
            }
            return null; // Don't show if no permission
        }

        // If it's a folder (no program), check if any children are visible
        List<MenuDTO> allowedChildren = new ArrayList<>();
        for (Menu child : menu.getChildren()) {
            if (!child.getIsVisible()) continue;
            MenuDTO childDTO = filterMenuTree(child, userProgramIds);
            if (childDTO != null) {
                allowedChildren.add(childDTO);
            }
        }

        if (allowedChildren.isEmpty()) {
            return null; // Don't show empty folders
        }

        MenuDTO dto = toDTO(menu);
        dto.setChildren(allowedChildren);
        return dto;
    }

    public MenuDTO createMenu(UUID parentId, UUID programId, String name, Integer sortOrder, Boolean isVisible, String path) {
        Menu parent = null;
        if (parentId != null) {
            parent = menuRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent not found"));
        }

        Program program = null;
        if (programId != null) {
            program = programRepository.findById(programId)
                    .orElseThrow(() -> new RuntimeException("Program not found"));
        }

        Menu menu = Menu.builder()
                .parent(parent)
                .program(program)
                .name(name)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isVisible(isVisible != null ? isVisible : true)
                .path(path)
                .build();

        return toDTO(menuRepository.save(menu));
    }

    public MenuDTO updateMenu(UUID id, UUID parentId, UUID programId, String name, Integer sortOrder, Boolean isVisible, String path) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu not found"));

        if (parentId != null) {
            if (id.equals(parentId)) {
                throw new RuntimeException("Cannot be its own parent");
            }
            Menu parent = menuRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent not found"));
            menu.setParent(parent);
        } else {
            menu.setParent(null);
        }

        if (programId != null) {
            Program program = programRepository.findById(programId)
                    .orElseThrow(() -> new RuntimeException("Program not found"));
            menu.setProgram(program);
        } else {
            menu.setProgram(null);
        }

        if (name != null) menu.setName(name);
        if (sortOrder != null) menu.setSortOrder(sortOrder);
        if (isVisible != null) menu.setIsVisible(isVisible);
        if (path != null) menu.setPath(path);

        return toDTO(menuRepository.save(menu));
    }

    public void deleteMenu(UUID id) {
        menuRepository.deleteById(id);
    }

    private MenuDTO toDTO(Menu menu) {
        MenuDTO dto = MenuDTO.builder()
                .id(menu.getId())
                .name(menu.getName())
                .parentId(menu.getParent() != null ? menu.getParent().getId() : null)
                .sortOrder(menu.getSortOrder())
                .isVisible(menu.getIsVisible())
                .programId(menu.getProgram() != null ? menu.getProgram().getId() : null)
                .path(menu.getPath())
                .programCode(menu.getProgram() != null ? menu.getProgram().getProgramCode() : null)
                .createdAt(menu.getCreatedAt())
                .updatedAt(menu.getUpdatedAt())
                .build();
        
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            dto.setChildren(menu.getChildren().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
