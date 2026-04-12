package com.example.ecommerce.dto;

import com.example.ecommerce.entity.ProgramType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDTO {
    private UUID id;
    private String category1;
    private String category2;
    private String programCode;
    private String name;
    private String url;
    private String httpMethod;
    private ProgramType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
