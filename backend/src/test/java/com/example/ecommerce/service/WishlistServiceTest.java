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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;

    @InjectMocks
    private WishlistService wishlistService;

    @Nested
    @DisplayName("찜 목록 조회 테스트")
    class GetWishlistTests {
        @Test
        @DisplayName("성공: 찜한 상품 목록과 대표 이미지를 매핑하여 반환한다")
        void getUserWishlists_success() {
            User user = User.builder().id(UUID.randomUUID()).build();
            User seller = User.builder().id(UUID.randomUUID()).name("Seller").build();
            Product product = Product.builder().id(UUID.randomUUID()).itemName("Prod").unitPrice(BigDecimal.TEN).seller(seller).build();
            Wishlist wishlist = Wishlist.builder().id(UUID.randomUUID()).product(product).build();

            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).willReturn(List.of(wishlist));
            
            ProductImage img = ProductImage.builder().product(product).imageUrl("img-url").build();
            given(productImageRepository.findByProductIdIn(anyList())).willReturn(List.of(img));

            List<WishlistDTO> result = wishlistService.getUserWishlists("user");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getImageUrl()).isEqualTo("img-url");
            assertThat(result.get(0).getSellerName()).isEqualTo("Seller");
        }
    }

    @Nested
    @DisplayName("찜 상태 토글 테스트")
    class ToggleTests {
        @Test
        @DisplayName("추가: 찜하지 않은 상품이면 새로 추가한다")
        void toggleWishlist_add() {
            User user = User.builder().id(UUID.randomUUID()).build();
            UUID productId = UUID.randomUUID();
            Product product = Product.builder().id(productId).build();

            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)).willReturn(false);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            wishlistService.toggleWishlist("user", productId);

            verify(wishlistRepository).save(any(Wishlist.class));
            verify(wishlistRepository, never()).deleteByUserIdAndProductId(any(), any());
        }

        @Test
        @DisplayName("삭제: 이미 찜한 상품이면 목록에서 제거한다")
        void toggleWishlist_remove() {
            User user = User.builder().id(UUID.randomUUID()).build();
            UUID productId = UUID.randomUUID();

            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)).willReturn(true);

            wishlistService.toggleWishlist("user", productId);

            verify(wishlistRepository).deleteByUserIdAndProductId(user.getId(), productId);
            verify(wishlistRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("찜 여부 확인 테스트")
    class CheckTests {
        @Test
        @DisplayName("성공: 찜한 상태인 경우 true를 반환한다")
        void isWishlisted_true() {
            User user = User.builder().id(UUID.randomUUID()).build();
            UUID productId = UUID.randomUUID();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)).willReturn(true);

            assertThat(wishlistService.isWishlisted("user", productId)).isTrue();
        }

        @Test
        @DisplayName("비인증: 사용자가 없으면 false를 반환한다")
        void isWishlisted_userNotFound_false() {
            given(userRepository.findByUsername("guest")).willReturn(Optional.empty());

            assertThat(wishlistService.isWishlisted("guest", UUID.randomUUID())).isFalse();
            verify(wishlistRepository, never()).existsByUserIdAndProductId(any(), any());
        }
    }
}
