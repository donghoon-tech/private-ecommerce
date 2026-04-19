package com.example.ecommerce.controller;

import com.example.ecommerce.dto.WishlistDTO;
import com.example.ecommerce.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistDTO>> getMyWishlists(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(wishlistService.getUserWishlists(userDetails.getUsername()));
    }

    @PostMapping("/{productId}/toggle")
    public ResponseEntity<Void> toggleWishlist(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        wishlistService.toggleWishlist(userDetails.getUsername(), productId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{productId}/check")
    public ResponseEntity<Map<String, Boolean>> checkWishlist(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("isWishlisted", false));
        }
        boolean isWishlisted = wishlistService.isWishlisted(userDetails.getUsername(), productId);
        return ResponseEntity.ok(Map.of("isWishlisted", isWishlisted));
    }
}
