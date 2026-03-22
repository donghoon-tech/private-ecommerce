package com.example.ecommerce.repository;

import com.example.ecommerce.entity.RoleMenuAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleMenuActionRepository extends JpaRepository<RoleMenuAction, UUID> {
    List<RoleMenuAction> findByRoleId(UUID roleId);
    List<RoleMenuAction> findByMenuId(UUID menuId);
    Optional<RoleMenuAction> findByRoleIdAndMenuId(UUID roleId, UUID menuId);
    void deleteByRoleId(UUID roleId);
    void deleteByMenuId(UUID menuId);

    // Using JPA Method Naming Strategy instead of @Query
    boolean existsByRole_NameAndMenu_IdAndCanReadTrue(String roleName, UUID menuId);
}
