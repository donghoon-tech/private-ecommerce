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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    /**
     * 사용자의 찜(Wishlist) 목록 최신순으로 조회합니다.
     * N+1 문제를 방지하기 위해 찜한 상품들의 이미지를 한 번의 쿼리(Batch Fetching)로 조회하여 조합하는 최적화가 적용되어 있습니다.
     *
     * @param username 찜 목록을 조회할 대상 사용자의 식별자
     * @return 사용자의 찜 목록 리스트 (WishlistDTO)
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getUserWishlists(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<UUID> productIds = wishlists.stream().map(w -> w.getProduct().getId()).collect(Collectors.toList());
        Map<UUID, String> productImagesMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductImage> images = productImageRepository.findByProductIdIn(productIds);
            for (ProductImage image : images) {
                if (!productImagesMap.containsKey(image.getProduct().getId())) {
                    productImagesMap.put(image.getProduct().getId(), image.getImageUrl());
                }
            }
        }
        
        return wishlists.stream().map(w -> {
            Product p = w.getProduct();
            String imageUrl = productImagesMap.getOrDefault(p.getId(), "");
                
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

    /**
     * 특정 상품에 대해 찜하기 상태를 토글(추가/삭제)합니다.
     * 해당 사용자가 이 상품을 찜하지 않았다면 새로 추가하고, 이미 찜한 상태라면 기존 항목을 삭제하여 상태를 반전시키는 특성이 있습니다.
     *
     * @param username 요청하는 사용자 식별자
     * @param productId 대상이 되는 상품의 ID
     */
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
    
    /**
     * 특정 사용자가 지정된 상품을 찜 목록에 추가했는지 여부를 반환합니다.
     * (UI 등에서 찜 버튼의 활성/비활성 여부를 결정할 때 활용됩니다.)
     *
     * @param username 대상 사용자 식별자
     * @param productId 찜 여부를 확인할 상품 ID
     * @return 찜한 이력이 있으면 true, 아니면 false 반환 (사용자 미존재 시에도 false 반환)
     */
    @Transactional(readOnly = true)
    public boolean isWishlisted(String username, UUID productId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }
}
