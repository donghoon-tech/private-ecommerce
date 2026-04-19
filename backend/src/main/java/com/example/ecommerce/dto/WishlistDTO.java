package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDTO {
    private UUID id;
    private UUID productId;
    private String itemName;
    private BigDecimal unitPrice;
    private String sellerName;
    private String imageUrl;
    private LocalDateTime createdAt;
}
