package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductDTO;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductImage;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.repository.BusinessProfileRepository;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("전체 상품 목록 조회 시, 상품 정보와 함께 이미지 및 판매자 정보가 매핑되어 반환된다")
    void getAllProducts_success() {
        // Given
        UUID sellerId = UUID.randomUUID();
        User seller = User.builder().id(sellerId).name("SellerUser").build();

        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .seller(seller)
                .itemName("Test Product")
                .isDisplayed(true)
                .build();

        ProductImage image = ProductImage.builder().product(product).imageUrl("http://img.com/1.jpg").displayOrder(1)
                .build();
        BusinessProfile profile = BusinessProfile.builder().user(seller).businessName("Best Seller").isMain(true)
                .build();
        ProductDTO expectedDTO = ProductDTO.builder().id(productId).itemName("Test Product").sellerName("Best Seller")
                .build();

        given(productRepository.searchProducts(null, null)).willReturn(List.of(product));
        given(productImageRepository.findByProductIdIn(anyList())).willReturn(List.of(image));
        given(businessProfileRepository.findByUserIdIn(anyList())).willReturn(List.of(profile));
        given(productMapper.toDTO(any(Product.class), anyString(), anyList())).willReturn(expectedDTO);

        // When
        List<ProductDTO> result = productService.getAllProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSellerName()).isEqualTo("Best Seller");

        then(productRepository).should(times(1)).searchProducts(null, null);
        then(productImageRepository).should(times(1)).findByProductIdIn(anyList());
        then(businessProfileRepository).should(times(1)).findByUserIdIn(anyList());
    }

    @Test
    @DisplayName("상품 상세 조회 시, 해당 상품이 존재하면 DTO를 반환한다")
    void getProductById_success() {
        // Given
        UUID sellerId = UUID.randomUUID();
        User seller = User.builder().id(sellerId).name("SellerUser").build();

        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).seller(seller).itemName("Detail Product").build();
        ProductDTO expectedDTO = ProductDTO.builder().id(productId).itemName("Detail Product").build();

        given(productRepository.findWithDetailsById(productId)).willReturn(Optional.of(product));
        // toProductDTOs 내부 로직 Mocking (단건 조회지만 내부적으로 Bulk 메서드 재사용 중)
        given(productImageRepository.findByProductIdIn(anyList())).willReturn(Collections.emptyList());
        given(businessProfileRepository.findByUserIdIn(anyList())).willReturn(Collections.emptyList());
        given(productMapper.toDTO(any(Product.class), anyString(), anyList())).willReturn(expectedDTO);

        // When
        ProductDTO result = productService.getProductById(productId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItemName()).isEqualTo("Detail Product");
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 조회 시 예외가 발생한다")
    void getProductById_notFound() {
        // Given
        UUID invalidId = UUID.randomUUID();
        given(productRepository.findWithDetailsById(invalidId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(invalidId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found");
    }
}
