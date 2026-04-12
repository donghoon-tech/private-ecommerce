package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    // '신재', '고재'
    @Column(name = "item_condition", nullable = false, length = 20)
    private String itemCondition;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "sale_unit", length = 50)
    private String saleUnit;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "loading_address", nullable = false, columnDefinition = "TEXT")
    private String loadingAddress;

    @Column(name = "loading_address_display", nullable = false, length = 200)
    private String loadingAddressDisplay;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "is_displayed", nullable = false)
    @Builder.Default
    private boolean isDisplayed = true;

    public enum Status {
        PENDING, APPROVED, REJECTED, SELLING, SOLD_OUT
    }
}
