package com.example.ecommerce.service;

import com.example.ecommerce.dto.WishlistDTO;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductImage;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.Wishlist;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional(readOnly = true)
    public List<WishlistDTO> getUserWishlists(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        return wishlists.stream().map(w -> {
            Product p = w.getProduct();
            String imageUrl = productImageRepository.findByProductId(p.getId())
                .stream().findFirst().map(ProductImage::getImageUrl).orElse("");
                
            return WishlistDTO.builder()
                .id(w.getId())
                .productId(p.getId())
                .itemName(p.getItemName())
                .unitPrice(p.getUnitPrice())
                .sellerName(p.getSeller().getName())
                .imageUrl(imageUrl)
                .createdAt(w.getCreatedAt())
                .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void toggleWishlist(String username, UUID productId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
        } else {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                
            Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
            wishlistRepository.save(wishlist);
        }
    }
    
    @Transactional(readOnly = true)
    public boolean isWishlisted(String username, UUID productId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }
}
