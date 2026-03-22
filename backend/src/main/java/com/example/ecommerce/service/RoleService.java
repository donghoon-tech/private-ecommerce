package com.example.ecommerce.service;


import com.example.ecommerce.dto.RoleDTO;
import com.example.ecommerce.dto.RoleMenuActionDTO;
import com.example.ecommerce.dto.RoleMenuActionRequest;
import com.example.ecommerce.entity.Menu;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.RoleMenuAction;
import com.example.ecommerce.repository.MenuRepository;
import com.example.ecommerce.repository.RoleMenuActionRepository;
import com.example.ecommerce.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final RoleMenuActionRepository roleMenuActionRepository;

    /**
     * 전체 Role 목록 조회 (각 Role에 바인딩된 Permission 포함)
     */
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 특정 Role 조회
     */
    @Transactional(readOnly = true)
    public RoleDTO getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("해당 Role을 찾을 수 없습니다."));
        return toDTO(role);
    }

    /**
     * 전체 Permission 목록 조회 (읽기 전용)
     */

    /**
     * Role 생성
     */
    public RoleDTO createRole(String name, String description, List<RoleMenuActionRequest> actionRequests) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new RuntimeException("이미 존재하는 Role 이름입니다: " + name);
        }

        Role role = Role.builder()
                .name(name.toUpperCase())
                .description(description)
                .build();
        role = roleRepository.save(role);

        Set<RoleMenuAction> menuActions = resolveRoleMenuActions(role, actionRequests);
        role.setMenuActions(menuActions);

        return toDTO(roleRepository.save(role));
    }

    /**
     * Role의 Permission 바인딩 수정 (이름, 설명 포함)
     */
    public RoleDTO updateRole(UUID roleId, String name, String description, List<RoleMenuActionRequest> actionRequests) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("해당 Role을 찾을 수 없습니다."));

        if (name != null && !name.isBlank()) {
            role.setName(name.toUpperCase());
        }
        if (description != null) {
            role.setDescription(description);
        }
        if (actionRequests != null) {
            role.getMenuActions().clear();
            role.getMenuActions().addAll(resolveRoleMenuActions(role, actionRequests));
        }

        return toDTO(roleRepository.save(role));
    }

    /**
     * Role 삭제 (기본 Role인 UNVERIFIED, USER, ADMIN은 삭제 불가)
     */
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("해당 Role을 찾을 수 없습니다."));

        List<String> protectedRoles = List.of("UNVERIFIED", "USER", "ADMIN");
        if (protectedRoles.contains(role.getName())) {
            throw new RuntimeException("기본 Role(" + role.getName() + ")은 삭제할 수 없습니다.");
        }

        roleRepository.delete(role);
    }

    private Set<RoleMenuAction> resolveRoleMenuActions(Role role, List<RoleMenuActionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new HashSet<>();
        }
        Set<RoleMenuAction> actions = new HashSet<>();
        for (RoleMenuActionRequest req : requests) {
            Menu menu = menuRepository.findById(req.getMenuId())
                    .orElseThrow(() -> new RuntimeException("메뉴를 찾을 수 없습니다 ID: " + req.getMenuId()));
            
            // Check if action already exists for this role and menu to avoid duplicate key error
            RoleMenuAction action = roleMenuActionRepository.findByRoleIdAndMenuId(role.getId(), menu.getId())
                    .orElseGet(() -> RoleMenuAction.builder()
                            .role(role)
                            .menu(menu)
                            .build());

            action.setCanRead(req.isCanRead());
            action.setCanCreate(req.isCanCreate());
            action.setCanUpdate(req.isCanUpdate());
            action.setCanDelete(req.isCanDelete());
            action.setCanExcel(req.isCanExcel());
            
            actions.add(roleMenuActionRepository.save(action));
        }
        return actions;
    }

    private RoleDTO toDTO(Role role) {
        List<String> permStrings = new java.util.ArrayList<>();
        if (role.getMenuActions() != null) {
            for (RoleMenuAction action : role.getMenuActions()) {
                if (action.getMenu() != null && action.getMenu().getMenuCode() != null) {
                    String code = action.getMenu().getMenuCode().toUpperCase();
                    if (action.isCanRead()) permStrings.add(code + ":READ");
                    if (action.isCanCreate()) permStrings.add(code + ":CREATE");
                    if (action.isCanUpdate()) permStrings.add(code + ":UPDATE");
                    if (action.isCanDelete()) permStrings.add(code + ":DELETE");
                    if (action.isCanExcel()) permStrings.add(code + ":EXCEL");
                }
            }
        }
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permStrings)
                .build();
    }
}
