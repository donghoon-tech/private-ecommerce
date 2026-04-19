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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;

    @InjectMocks
    private CartService cartService;

    @Nested
    @DisplayName("장바구니 조회 테스트 (N+1 최적화 포함)")
    class GetCartTests {
        @Test
        @DisplayName("성공: 장바구니에 아이템이 있을 때, 이미지와 판매자 상호명을 일괄 매핑하여 반환한다")
        void getUserCart_success() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("user").build();
            User seller = User.builder().id(UUID.randomUUID()).name("Original Name").build();
            Product product = Product.builder()
                    .id(UUID.randomUUID())
                    .itemName("Test Product")
                    .unitPrice(BigDecimal.valueOf(1000))
                    .seller(seller)
                    .build();
            CartItem item = CartItem.builder().id(UUID.randomUUID()).product(product).quantity(2).build();

            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(cartItemRepository.findByUserId(user.getId())).willReturn(List.of(item));
            
            ProductImage img = ProductImage.builder().product(product).imageUrl("img-url").build();
            given(productImageRepository.findByProductIdIn(anyList())).willReturn(List.of(img));
            
            BusinessProfile profile = BusinessProfile.builder().user(seller).businessName("Business Name").isMain(true).build();
            given(businessProfileRepository.findByUserIdIn(anyList())).willReturn(List.of(profile));

            // When
            List<CartDTO> result = cartService.getUserCart("user");

            // Then
            assertThat(result).hasSize(1);
            CartDTO dto = result.get(0);
            assertThat(dto.getItemName()).isEqualTo("Test Product");
            assertThat(dto.getSellerName()).isEqualTo("Business Name");
            assertThat(dto.getImageUrl()).isEqualTo("img-url");
            assertThat(dto.getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("성공: 장바구니가 비어있을 때 빈 리스트를 반환한다")
        void getUserCart_empty() {
            User user = User.builder().id(UUID.randomUUID()).build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(cartItemRepository.findByUserId(user.getId())).willReturn(Collections.emptyList());

            List<CartDTO> result = cartService.getUserCart("user");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("장바구니 추가 테스트")
    class AddCartTests {
        @Test
        @DisplayName("성공: 동일 상품이 없으면 새로운 항목을 저장한다")
        void addToCart_newItem() {
            User user = User.builder().id(UUID.randomUUID()).build();
            User seller = User.builder().id(UUID.randomUUID()).name("Seller").build();
            Product product = Product.builder().id(UUID.randomUUID()).seller(seller).unitPrice(BigDecimal.ZERO).build();
            CartRequest request = new CartRequest();
            request.setProductId(product.getId());
            request.setQuantity(1);

            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
            given(cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())).willReturn(Optional.empty());

            cartService.addToCart("user", request);

            verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 1));
        }

        @Test
        @DisplayName("성공: 동일 상품이 이미 존재하면 수량만 증가시킨다")
        void addToCart_existingItem_mergeQuantity() {
            User user = User.builder().id(UUID.randomUUID()).build();
            Product product = Product.builder().id(UUID.randomUUID()).seller(User.builder().id(UUID.randomUUID()).build()).unitPrice(BigDecimal.ZERO).build();
            CartItem existingItem = CartItem.builder().quantity(2).build();
            CartRequest request = new CartRequest();
            request.setProductId(product.getId());
            request.setQuantity(3);

            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
            given(cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())).willReturn(Optional.of(existingItem));

            cartService.addToCart("user", request);

            assertThat(existingItem.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(existingItem);
        }
    }

    @Nested
    @DisplayName("수량 수정 및 삭제 테스트")
    class UpdateRemoveTests {
        @Test
        @DisplayName("수정 성공: 수량이 1 이상이면 업데이트한다")
        void updateQuantity_success() {
            User user = User.builder().id(UUID.randomUUID()).build();
            CartItem item = CartItem.builder().user(user).quantity(1).build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(cartItemRepository.findById(any())).willReturn(Optional.of(item));

            cartService.updateCartQuantity("user", UUID.randomUUID(), 10);

            assertThat(item.getQuantity()).isEqualTo(10);
            verify(cartItemRepository).save(item);
        }

        @Test
        @DisplayName("수정 자동삭제: 수량이 0 이하로 들어오면 항목을 삭제한다")
        void updateQuantity_deleteWhenZero() {
            User user = User.builder().id(UUID.randomUUID()).build();
            CartItem item = CartItem.builder().user(user).quantity(5).build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(cartItemRepository.findById(any())).willReturn(Optional.of(item));

            cartService.updateCartQuantity("user", UUID.randomUUID(), 0);

            verify(cartItemRepository).delete(item);
        }

        @Test
        @DisplayName("수정 권한 실패: 타인의 장바구니 항목 수정 시도 시 예외 발생")
        void updateQuantity_unauthorized() {
            User user = User.builder().id(UUID.randomUUID()).build();
            User other = User.builder().id(UUID.randomUUID()).build();
            CartItem item = CartItem.builder().user(other).build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(cartItemRepository.findById(any())).willReturn(Optional.of(item));

            assertThatThrownBy(() -> cartService.updateCartQuantity("user", UUID.randomUUID(), 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Not authorized");
        }

        @Test
        @DisplayName("전체 삭제: 사용자 ID 기반으로 모든 장바구니 항목을 제거한다")
        void clearCart_success() {
            User user = User.builder().id(UUID.randomUUID()).build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));

            cartService.clearCart("user");

            verify(cartItemRepository).deleteByUserId(user.getId());
        }
    }
}
