package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuActionDTO {
    private UUID id;
    private UUID menuId;
    private String menuCode;
    private String menuName;
    private UUID parentId;
    
    private boolean canRead;
    private boolean canCreate;
    private boolean canUpdate;
    private boolean canDelete;
    private boolean canExcel;
}
