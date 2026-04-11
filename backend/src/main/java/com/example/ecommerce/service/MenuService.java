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

import java.util.*;
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
        List<Menu> allMenus = menuRepository.findAllWithProgram();
        return buildTree(allMenus, null);
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getUserMenuTree(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Set<UUID> userProgramIds = user.getRole().getPrograms().stream()
                .map(Program::getId)
                .collect(Collectors.toSet());

        List<Menu> allMenus = menuRepository.findAllWithProgram();
        
        // Filter in memory to avoid N+1 and many DB calls
        List<Menu> filteredMenus = allMenus.stream()
                .filter(m -> m.getIsVisible())
                .filter(m -> m.getProgram() == null || userProgramIds.contains(m.getProgram().getId()))
                .collect(Collectors.toList());

        // Build tree from filtered list
        List<MenuDTO> tree = buildTree(filteredMenus, userProgramIds);
        return tree;
    }

    private List<MenuDTO> buildTree(List<Menu> allMenus, Set<UUID> userProgramIds) {
        Map<UUID, MenuDTO> dtoMap = new LinkedHashMap<>();
        
        // First convert all to DTO
        for (Menu menu : allMenus) {
            dtoMap.put(menu.getId(), toDTOOnly(menu));
        }

        List<MenuDTO> roots = new ArrayList<>();
        for (Menu menu : allMenus) {
            MenuDTO current = dtoMap.get(menu.getId());
            if (menu.getParent() == null) {
                roots.add(current);
            } else {
                MenuDTO parent = dtoMap.get(menu.getParent().getId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(current);
                }
            }
        }

        // If filtering by programs, we need to prune branches with no leaves
        if (userProgramIds != null) {
            return pruneEmptyNodes(roots);
        }

        return roots;
    }

    private List<MenuDTO> pruneEmptyNodes(List<MenuDTO> nodes) {
        List<MenuDTO> result = new ArrayList<>();
        for (MenuDTO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                List<MenuDTO> validChildren = pruneEmptyNodes(node.getChildren());
                if (!validChildren.isEmpty()) {
                    node.setChildren(validChildren);
                    result.add(node);
                }
            } else if (node.getProgramId() != null) {
                // It's a valid leaf
                result.add(node);
            }
        }
        return result;
    }

    private MenuDTO toDTOOnly(Menu menu) {
        return MenuDTO.builder()
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

        return toDTOWithChildren(menuRepository.save(menu));
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

        return toDTOWithChildren(menuRepository.save(menu));
    }

    public void deleteMenu(UUID id) {
        menuRepository.deleteById(id);
    }

    private MenuDTO toDTOWithChildren(Menu menu) {
        MenuDTO dto = toDTOOnly(menu);
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            dto.setChildren(menu.getChildren().stream()
                    .map(this::toDTOWithChildren)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
