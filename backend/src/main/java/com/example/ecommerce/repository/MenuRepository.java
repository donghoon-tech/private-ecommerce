package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuRepository extends JpaRepository<Menu, UUID> {
    List<Menu> findByParentIsNullOrderBySortOrderAsc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"program", "parent"})
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Menu m ORDER BY m.sortOrder ASC")
    List<Menu> findAllWithProgram();
}
