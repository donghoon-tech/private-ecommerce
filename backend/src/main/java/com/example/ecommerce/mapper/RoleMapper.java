package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.ProgramDTO;
import com.example.ecommerce.dto.RoleDTO;
import com.example.ecommerce.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleMapper {
    private final ProgramMapper programMapper;

    public RoleDTO toDTO(Role role) {
        if (role == null) return null;

        List<ProgramDTO> programDTOs = role.getPrograms().stream()
                .map(programMapper::toDTO)
                .collect(Collectors.toList());

        List<String> permissions = role.getPrograms().stream()
                .map(p -> p.getProgramCode().toUpperCase())
                .collect(Collectors.toList());

        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .programs(programDTOs)
                .permissions(permissions)
                .build();
    }
}
