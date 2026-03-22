package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import com.example.ecommerce.entity.RoleMenuAction;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    // password 필드는 DTO에 없으므로 매핑 설정 불필요
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "representativePhone", source = "user.representativePhone")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "user.role.name")
    @Mapping(target = "roleDescription", source = "user.role.description")
    @Mapping(target = "permissions", expression = "java(mapMenuActions(user.getRole().getMenuActions()))")
    @Mapping(target = "businessNumber", source = "user.businessNumber")
    @Mapping(target = "isActive", source = "user.active") // Lombok @Builder.Default boolean isActive -> isActive() ->
                                                          // 'active' property
    @Mapping(target = "createdAt", source = "user.createdAt")
    // Business Profile Fields
    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "companyName", source = "profile.businessName")
    @Mapping(target = "officeAddress", source = "profile.officeAddress")
    @Mapping(target = "storageAddress", source = "profile.storageAddress")
    @Mapping(target = "businessStatus", source = "profile.status")
    @Mapping(target = "rejectionReason", source = "profile.rejectionReason")
    UserDTO toDTO(User user, BusinessProfile profile);

    // Profile 없이 User만 있을 경우
    @Mapping(target = "profileId", ignore = true)
    @Mapping(target = "companyName", ignore = true)
    @Mapping(target = "officeAddress", ignore = true)
    @Mapping(target = "storageAddress", ignore = true)
    @Mapping(target = "businessStatus", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "isActive", source = "user.active")
    @Mapping(target = "role", source = "user.role.name")
    @Mapping(target = "roleDescription", source = "user.role.description")
    @Mapping(target = "permissions", expression = "java(mapMenuActions(user.getRole().getMenuActions()))")
    UserDTO toDTO(User user);

    default List<String> mapMenuActions(java.util.Set<RoleMenuAction> actions) {
        if (actions == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<String> perms = new java.util.ArrayList<>();
        for (RoleMenuAction action : actions) {
            if (action.getMenu() != null && action.getMenu().getMenuCode() != null) {
                String c = action.getMenu().getMenuCode().toUpperCase();
                if (action.isCanRead()) perms.add(c + ":READ");
                if (action.isCanCreate()) perms.add(c + ":CREATE");
                if (action.isCanUpdate()) perms.add(c + ":UPDATE");
                if (action.isCanDelete()) perms.add(c + ":DELETE");
                if (action.isCanExcel()) perms.add(c + ":EXCEL");
            }
        }
        return perms;
    }
}
