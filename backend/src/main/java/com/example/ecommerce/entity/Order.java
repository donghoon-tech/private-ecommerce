package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "order_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Column(name = "truck_tonnage", length = 20)
    private String truckTonnage;

    @Column(name = "truck_type", length = 20)
    @Enumerated(EnumType.STRING)
    private TruckType truckType;

    @Column(name = "shipping_loading_address", nullable = false, columnDefinition = "TEXT")
    private String shippingLoadingAddress;

    @Column(name = "shipping_unloading_address", nullable = false, columnDefinition = "TEXT")
    private String shippingUnloadingAddress;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "payment_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "order_memo", columnDefinition = "TEXT")
    private String orderMemo;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Column(name = "delivery_started_at")
    private LocalDateTime deliveryStartedAt;

    @Column(name = "delivery_completed_at")
    private LocalDateTime deliveryCompletedAt;

    @Column(name = "carrier_info", columnDefinition = "TEXT")
    private String carrierInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum OrderType {
        PLATFORM, PHONE
    }

    public enum TruckType {
        CARGO, WINGBODY
    }

    public enum Status {
        PENDING, SHIPPING, DELIVERED, COMPLETED
    }

    public enum PaymentStatus {
        PENDING, CONFIRMED, SETTLED
    }
}
