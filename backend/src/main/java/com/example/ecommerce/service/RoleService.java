package com.example.ecommerce.service;


import com.example.ecommerce.constant.ErrorMessage;
import com.example.ecommerce.dto.ProgramDTO;
import com.example.ecommerce.dto.RoleDTO;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.mapper.ProgramMapper;
import com.example.ecommerce.mapper.RoleMapper;
import com.example.ecommerce.repository.ProgramRepository;
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
    private final ProgramRepository programRepository;
    private final RoleMapper roleMapper;
    private final ProgramMapper programMapper;

    /**
     * 전체 권한(Role) 목록 및 각 권한에 할당된 프로그램(접근 가능 URL/Action) 정보를 함께 조회합니다.
     *
     * @return 권한 및 프로그램 정보가 포함된 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 식별자(UUID) 기반으로 특정 Role 단건의 상세 내역을 조회합니다.
     *
     * @param roleId 대상 권한의 식별자
     * @return 대상 권한 DTO
     * @throws RuntimeException 대상을 찾지 못한 경우 예외
     */
    @Transactional(readOnly = true)
    public RoleDTO getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(ErrorMessage.ROLE_NOT_FOUND));
        return roleMapper.toDTO(role);
    }

    /**
     * 시스템 전체 프로그램(메뉴, API 등) 목록을 조회합니다.
     * 프론트엔드에서 Role에 세부 권한을 부여할 때 선택 목록으로 활용됩니다.
     *
     * @return 전체 프로그램 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ProgramDTO> getAllPrograms() {
        return programRepository.findAll().stream()
                .map(programMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 새로운 기능 권한(Role)을 생성하고, 선택된 프로그램들을 연관관계로 즉시 매핑합니다.
     * Role 이름은 무조건 대문자로 변환되어 등록되며 중복 생성을 방지합니다.
     *
     * @param name Role 이름 (예: ADMIN_SUB)
     * @param description Role에 대한 설명
     * @param programIds 연관시킬 프로그램 ID 목록
     * @return 생성 완료된 권한 정보 DTO
     */
    public RoleDTO createRole(String name, String description, List<UUID> programIds) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new RuntimeException("이미 존재하는 Role 이름입니다: " + name);
        }

        Role role = Role.builder()
                .name(name.toUpperCase())
                .description(description)
                .build();
        role = roleRepository.save(role);

        Set<Program> programs = resolvePrograms(programIds);
        role.setPrograms(programs);

        return roleMapper.toDTO(roleRepository.save(role));
    }

    /**
     * 기존 권한(Role)의 기본 정보(이름, 설명) 및 프로그램 할당 내역을 수정합니다.
     * 프로그램 ID 목록이 주어질 경우 기존 할당은 모두 제거되고 새로운 목록으로 완전히 덮어씌워(Override) 집니다.
     *
     * @param roleId 대상 권한 ID
     * @param name 수정할 이름 (null 허용, 대문자 변환됨)
     * @param description 수정할 설명
     * @param programIds 새로 덮어씌울 프로그램 ID 목록
     * @return 수정 반영된 DTO
     */
    public RoleDTO updateRole(UUID roleId, String name, String description, List<UUID> programIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(ErrorMessage.ROLE_NOT_FOUND));

        if (name != null && !name.isBlank()) {
            role.setName(name.toUpperCase());
        }
        if (description != null) {
            role.setDescription(description);
        }
        if (programIds != null) {
            role.getPrograms().clear();
            role.getPrograms().addAll(resolvePrograms(programIds));
        }

        return roleMapper.toDTO(roleRepository.save(role));
    }

    /**
     * 특정 Role에 여러 프로그램을 추가(병합)로 할당합니다. 기존 할당된 내역은 보존됩니다.
     *
     * @param roleId 권한 ID
     * @param programIds 추가할 프로그램 ID 목록
     * @return 병합 할당된 결과 권한 DTO
     */
    public RoleDTO assignPrograms(UUID roleId, List<UUID> programIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(ErrorMessage.ROLE_NOT_FOUND));
        
        Set<Program> newPrograms = resolvePrograms(programIds);
        role.getPrograms().addAll(newPrograms);
        
        return roleMapper.toDTO(roleRepository.save(role));
    }

    /**
     * 특정 Role에서 할당된 일부 프로그램들을 회수(해제) 처리합니다.
     *
     * @param roleId 권한 ID
     * @param programIds 회수할 대상 제한 프로그램 ID 목록
     * @return 회수 후 결과 권한 DTO
     */
    public RoleDTO removePrograms(UUID roleId, List<UUID> programIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(ErrorMessage.ROLE_NOT_FOUND));
                
        Set<Program> programsToRemove = resolvePrograms(programIds);
        role.getPrograms().removeAll(programsToRemove);
        
        return roleMapper.toDTO(roleRepository.save(role));
    }

    /**
     * 권한(Role)을 시스템에서 삭제 처리합니다.
     * 'UNVERIFIED', 'USER', 'ADMIN'과 같은 시스템 운영 필수 코어 Role은 삭제할 수 없도록 강제 방어 처리 되어 있습니다.
     *
     * @param roleId 삭제할 권한의 식별자
     */
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(ErrorMessage.ROLE_NOT_FOUND));

        List<String> protectedRoles = List.of("UNVERIFIED", "USER", "ADMIN");
        if (protectedRoles.contains(role.getName())) {
            throw new RuntimeException("기본 Role(" + role.getName() + ")은 삭제할 수 없습니다.");
        }

        roleRepository.delete(role);
    }

    private Set<Program> resolvePrograms(List<UUID> programIds) {
        if (programIds == null || programIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(programRepository.findAllById(programIds));
    }
}
