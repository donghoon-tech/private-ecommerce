package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.ProgramDTO;
import com.example.ecommerce.entity.Program;
import org.springframework.stereotype.Component;

@Component
public class ProgramMapper {
    public ProgramDTO toDTO(Program program) {
        if (program == null) return null;
        return ProgramDTO.builder()
                .id(program.getId())
                .category1(program.getCategory1())
                .category2(program.getCategory2())
                .programCode(program.getProgramCode())
                .name(program.getName())
                .url(program.getUrl())
                .httpMethod(program.getHttpMethod())
                .type(program.getType())
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }

    public Program toEntity(ProgramDTO dto) {
        if (dto == null) return null;
        return Program.builder()
                .id(dto.getId())
                .category1(dto.getCategory1())
                .category2(dto.getCategory2())
                .programCode(dto.getProgramCode())
                .name(dto.getName())
                .url(dto.getUrl())
                .httpMethod(dto.getHttpMethod())
                .type(dto.getType())
                .build();
    }
}
