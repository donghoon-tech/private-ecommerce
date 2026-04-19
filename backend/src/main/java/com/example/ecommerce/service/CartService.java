package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartDTO;
import com.example.ecommerce.dto.request.CartRequest;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductImage;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Transactional(readOnly = true)
    public List<CartDTO> getUserCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        
        return cartItems.stream().map(c -> {
            Product p = c.getProduct();
            String imageUrl = productImageRepository.findByProductId(p.getId())
                .stream().findFirst().map(ProductImage::getImageUrl).orElse("");
                
            return CartDTO.builder()
                .id(c.getId())
                .productId(p.getId())
                .itemName(p.getItemName())
                .productSlug(p.getId().toString())
                .unitPrice(p.getUnitPrice())
                .sellerName(p.getSeller().getName())
                .imageUrl(imageUrl)
                .quantity(c.getQuantity())
                .build();
        }).collect(Collectors.toList());
    }

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
                
        return CartDTO.builder()
            .id(cartItem.getId())
            .productId(product.getId())
            .itemName(product.getItemName())
            .productSlug(product.getId().toString())
            .unitPrice(product.getUnitPrice())
            .sellerName(product.getSeller().getName())
            .imageUrl(imageUrl)
            .quantity(cartItem.getQuantity())
            .build();
    }

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

    @Transactional
    public void clearCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        cartItemRepository.deleteByUserId(user.getId());
    }
}
