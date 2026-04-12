package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Channel {
        SMS, EMAIL
    }

    public enum Status {
        PENDING, SENT, FAILED
    }
}
