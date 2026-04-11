package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuDTO {
    private UUID id;
    private String name;
    private UUID parentId;
    private Integer sortOrder;
    private Boolean isVisible;
    private UUID programId;
    private String path;
    private String programCode;
    private List<MenuDTO> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
