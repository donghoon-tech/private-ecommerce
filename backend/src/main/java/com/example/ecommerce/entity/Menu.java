package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // 논리적 메뉴 코드 (수동 권한 체킹용, 예: SYS_AUTH)
    @Column(name = "menu_code", unique = true, length = 50)
    private String menuCode;

    @Column(nullable = false, length = 100)
    private String name;

    // 자체 참조 (부모 식별자) - 최상위 메뉴는 null
    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    @JsonProperty("isVisible")
    private boolean isVisible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
