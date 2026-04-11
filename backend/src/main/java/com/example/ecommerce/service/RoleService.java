package com.example.ecommerce.service;


import com.example.ecommerce.dto.ProgramDTO;
import com.example.ecommerce.dto.RoleDTO;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.entity.Role;
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

    /**
     * 전체 Role 목록 조회 (각 Role에 바인딩된 Program 포함)
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
     * 전체 Program 목록 조회 (읽기 전용)
     */
    @Transactional(readOnly = true)
    public List<ProgramDTO> getAllPrograms() {
        return programRepository.findAll().stream()
                .map(this::toProgramDTO)
                .collect(Collectors.toList());
    }

    /**
     * Role 생성
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

        return toDTO(roleRepository.save(role));
    }

    /**
     * Role의 Program 바인딩 수정 (이름, 설명 포함)
     */
    public RoleDTO updateRole(UUID roleId, String name, String description, List<UUID> programIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("해당 Role을 찾을 수 없습니다."));

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

        return toDTO(roleRepository.save(role));
    }

    /**
     * Role에 Program 다중 할당
     */
    public RoleDTO assignPrograms(UUID roleId, List<UUID> programIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("해당 Role을 찾을 수 없습니다."));
        
        Set<Program> newPrograms = resolvePrograms(programIds);
        role.getPrograms().addAll(newPrograms);
        
        return toDTO(roleRepository.save(role));
    }

    /**
     * Role에서 Program 다중 회수
     */
    public RoleDTO removePrograms(UUID roleId, List<UUID> programIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("해당 Role을 찾을 수 없습니다."));
                
        Set<Program> programsToRemove = resolvePrograms(programIds);
        role.getPrograms().removeAll(programsToRemove);
        
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

    private Set<Program> resolvePrograms(List<UUID> programIds) {
        if (programIds == null || programIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(programRepository.findAllById(programIds));
    }

    private RoleDTO toDTO(Role role) {
        List<ProgramDTO> programDTOs = role.getPrograms().stream()
                .map(this::toProgramDTO)
                .collect(Collectors.toList());

        List<String> permStrings = role.getPrograms().stream()
                .map(p -> p.getProgramCode().toUpperCase())
                .collect(Collectors.toList());

        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .programs(programDTOs)
                .permissions(permStrings)
                .build();
    }

    private ProgramDTO toProgramDTO(Program program) {
        return ProgramDTO.builder()
                .id(program.getId())
                .category1(program.getCategory1())
                .category2(program.getCategory2())
                .programCode(program.getProgramCode())
                .name(program.getName())
                .url(program.getUrl())
                .type(program.getType())
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }
}
