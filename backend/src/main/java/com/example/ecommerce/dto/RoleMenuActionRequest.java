package com.example.ecommerce.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class RoleMenuActionRequest {
    private UUID menuId;
    private boolean canRead;
    private boolean canCreate;
    private boolean canUpdate;
    private boolean canDelete;
    private boolean canExcel;
}
