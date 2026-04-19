package com.example.ecommerce.service;

import com.example.ecommerce.dto.MenuDTO;
import com.example.ecommerce.entity.Menu;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.entity.ProgramType;
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
        Set<UUID> userProgramIds = new HashSet<>();
        
        if (username != null) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            userProgramIds = user.getRole().getPrograms().stream()
                    .map(Program::getId)
                    .collect(Collectors.toSet());
        }

        List<Menu> allMenus = menuRepository.findAllWithProgram();
        
        final Set<UUID> finalProgramIds = userProgramIds;
        List<Menu> filteredMenus = allMenus.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsVisible()))
                .filter(m -> m.getProgram() == null || m.getProgram().isPublic() || finalProgramIds.contains(m.getProgram().getId()))
                .collect(Collectors.toList());

        // Build tree from filtered list
        List<MenuDTO> tree = buildTree(filteredMenus, null); 
        return pruneEmptyNodes(tree);
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

        return roots;
    }

    private List<MenuDTO> pruneEmptyNodes(List<MenuDTO> nodes) {
        List<MenuDTO> result = new ArrayList<>();
        for (MenuDTO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                // 부모 노드: 유효한 자식이 하나라도 있어야 포함
                List<MenuDTO> validChildren = pruneEmptyNodes(node.getChildren());
                if (!validChildren.isEmpty()) {
                    node.setChildren(validChildren);
                    result.add(node);
                }
            } else {
                // 리프 노드 (혹은 자식이 다 잘려나간 부모)
                // 1. 연결된 프로그램이 있다면 권한 검증을 통과한 것이므로 유지
                if (node.getProgramId() != null) {
                    result.add(node);
                } 
                // 2. 연결된 프로그램이 없다면, URL이 유효한 경로(공개 메뉴)일 때만 유지
                else if (node.getPath() != null && !node.getPath().equals("#") && !node.getPath().isBlank()) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    private MenuDTO toDTOOnly(Menu menu) {
        // path 결정 우선순위:
        // 1. 메뉴에 직접 지정된 path가 있으면 그것을 사용
        // 2. 없고 WEB 타입 프로그램이 연결되어 있으면 → 프로그램 URL에서 자동으로 파생
        //    (단일 진실 공급원: programs.url이 FE 라우트 경로를 관리)
        // 3. 둘 다 없으면 null
        String path = menu.getPath();
        if ((path == null || path.isBlank()) && menu.getProgram() != null
                && menu.getProgram().getType() == ProgramType.WEB) {
            path = menu.getProgram().getUrl();
        }

        return MenuDTO.builder()
                .id(menu.getId())
                .name(menu.getName())
                .parentId(menu.getParent() != null ? menu.getParent().getId() : null)
                .sortOrder(menu.getSortOrder())
                .isVisible(menu.getIsVisible())
                .programId(menu.getProgram() != null ? menu.getProgram().getId() : null)
                .path(path)
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
