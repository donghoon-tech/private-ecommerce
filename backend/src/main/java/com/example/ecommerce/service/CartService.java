package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartDTO;
import com.example.ecommerce.dto.request.CartRequest;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductImage;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.BusinessProfileRepository;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BusinessProfileRepository businessProfileRepository;

    /**
     * 사용자의 장바구니 목록을 조회합니다.
     * 상품 이미지 및 판매자 정보를 일괄 조회(Batch Fetching)하여 N+1 문제를 완화하는 최적화가 적용되어 있습니다.
     *
     * @param username 장바구니를 조회할 사용자 식별자
     * @return 장바구니 상품 정보 목록
     */
    @Transactional(readOnly = true)
    public List<CartDTO> getUserCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        
        List<UUID> productIds = cartItems.stream().map(c -> c.getProduct().getId()).collect(Collectors.toList());
        Map<UUID, String> productImagesMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductImage> images = productImageRepository.findByProductIdIn(productIds);
            for (ProductImage image : images) {
                if (!productImagesMap.containsKey(image.getProduct().getId())) {
                    productImagesMap.put(image.getProduct().getId(), image.getImageUrl());
                }
            }
        }
        
        List<UUID> sellerIds = cartItems.stream().map(c -> c.getProduct().getSeller().getId()).distinct().collect(Collectors.toList());
        Map<UUID, String> sellerNameMap = new HashMap<>();
        if (!sellerIds.isEmpty()) {
            List<BusinessProfile> profiles = businessProfileRepository.findByUserIdIn(sellerIds);
            for (BusinessProfile profile : profiles) {
                if (profile.isMain()) {
                    sellerNameMap.put(profile.getUser().getId(), profile.getBusinessName());
                }
            }
        }

        return cartItems.stream().map(c -> {
            Product p = c.getProduct();
            String imageUrl = productImagesMap.getOrDefault(p.getId(), "");
            String sellerName = sellerNameMap.getOrDefault(p.getSeller().getId(), p.getSeller().getName());
                
            return CartDTO.builder()
                .id(c.getId())
                .productId(p.getId())
                .itemName(p.getItemName())
                .productSlug(p.getId().toString())
                .unitPrice(p.getUnitPrice())
                .sellerName(sellerName)
                .imageUrl(imageUrl)
                .quantity(c.getQuantity())
                .build();
        }).collect(Collectors.toList());
    }

    /**
     * 장바구니에 새로운 상품을 추가합니다.
     * 장바구니에 이미 동일한 상품이 존재하는 경우, 새로운 항목을 생성하지 않고 기존 항목의 수량을 추가합니다.
     *
     * @param username 상품을 추가할 사용자 식별자
     * @param request 장바구니 추가 요청 정보 (대상 상품 ID 및 수량)
     * @return 추가 또는 변경 처리된 장바구니 항목의 상세 정보
     */
    @Transactional
    public CartDTO addToCart(String username, CartRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Optional<CartItem> existingItemOptional = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        
        CartItem cartItem;
        if (existingItemOptional.isPresent()) {
            cartItem = existingItemOptional.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        } else {
            cartItem = CartItem.builder()
                .user(user)
                .product(product)
                .seller(product.getSeller())
                .quantity(request.getQuantity())
                .build();
        }
        
        cartItemRepository.save(cartItem);
        
        String imageUrl = productImageRepository.findByProductId(product.getId())
                .stream().findFirst().map(ProductImage::getImageUrl).orElse("");
                
        String sellerName = product.getSeller().getName();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(product.getSeller().getId());
        for (BusinessProfile profile : profiles) {
            if (profile.isMain()) {
                sellerName = profile.getBusinessName();
                break;
            }
        }

        return CartDTO.builder()
            .id(cartItem.getId())
            .productId(product.getId())
            .itemName(product.getItemName())
            .productSlug(product.getId().toString())
            .unitPrice(product.getUnitPrice())
            .sellerName(sellerName)
            .imageUrl(imageUrl)
            .quantity(cartItem.getQuantity())
            .build();
    }

    /**
     * 장바구니 내 특정 상품의 수량을 변경합니다.
     * 변경 요청된 수량이 0 이하인 경우, 해당 장바구니 항목을 삭제 처리하는 특이사항이 있습니다.
     * 본인의 장바구니 항목인지에 대한 권한 검증을 수행합니다.
     *
     * @param username 권한 검증에 사용될 요청 사용자 식별자
     * @param cartItemId 수량을 변경할 장바구니 항목 ID
     * @param quantity 변경할 수량
     * @throws IllegalArgumentException 항목을 찾을 수 없거나 대상 항목의 소유자가 아닌 경우
     */
    @Transactional
    public void updateCartQuantity(String username, UUID cartItemId, int quantity) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
                
        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not authorized to update this cart item");
        }
        
        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }
    }

    /**
     * 장바구니에서 특정 항목을 삭제합니다.
     * 본인의 장바구니 항목인지에 대한 권한 검증을 수행합니다.
     *
     * @param username 권한 검증에 사용될 요청 사용자 식별자
     * @param cartItemId 삭제할 장바구니 항목 ID
     * @throws IllegalArgumentException 항목을 찾을 수 없거나 대상 항목의 소유자가 아닌 경우
     */
    @Transactional
    public void removeFromCart(String username, UUID cartItemId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
                
        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not authorized to remove this cart item");
        }
        
        cartItemRepository.delete(cartItem);
    }

    /**
     * 사용자의 장바구니에 있는 모든 항목을 일괄 삭제(비우기)합니다.
     *
     * @param username 전체 삭제할 장바구니 소유자의 사용자 식별자
     */
    @Transactional
    public void clearCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        cartItemRepository.deleteByUserId(user.getId());
    }
}
