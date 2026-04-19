package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductDTO;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductImage;
import com.example.ecommerce.repository.BusinessProfileRepository;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
        private final ProductRepository productRepository;
        private final ProductImageRepository productImageRepository;
        private final BusinessProfileRepository businessProfileRepository;
        private final ProductMapper productMapper;

        /**
     * 시스템에 등록된 전체 상품 목록을 조회합니다.
     * 상품 이미지와 판매자 프로필 정보를 모아서 한 번에 조회(Batch Fetching)하여 N+1 성능 지연을 회피합니다.
     *
     * @return 전체 상품 DTO 리스트
     */
    public List<ProductDTO> getAllProducts() {
                List<Product> products = productRepository.searchProducts(null, null);
                return toProductDTOs(products);
        }

        /**
     * 특정 판매자가 등록한 상품 목록을 반환합니다.
     *
     * @param sellerId 대상 판매자의 식별자(UUID)
     * @return 대상 판매자의 상품 DTO 리스트
     */
    public List<ProductDTO> getProductsBySeller(UUID sellerId) {
                List<Product> products = productRepository.findBySellerId(sellerId);
                return toProductDTOs(products);
        }

        /**
     * 지정된 조건에 부합하는 상품을 검색하여 반환합니다.
     *
     * @param categoryId 필터링할 카테고리 ID (null 허용)
     * @param itemCondition 상품 상태 조건 (null 허용)
     * @return 검색된 상품 DTO 리스트
     */
    public List<ProductDTO> searchProducts(UUID categoryId, String itemCondition) {
                List<Product> products = productRepository.searchProducts(categoryId, itemCondition);
                return toProductDTOs(products);
        }

        /**
     * 상품 ID를 통해 단일 상품에 대한 상세 정보를 반환합니다.
     * 예외적으로 이 메서드도 내부적으로 다중 이미지 및 판매자 프로필 정보를 묶어 매핑하여 반환에 이용합니다.
     *
     * @param id 상세 정보를 확인할 상품의 식별자(UUID)
     * @return 상품 상세 내역 정보 DTO
     * @throws RuntimeException 매칭되는 상품이 없을 경우
     */
    public ProductDTO getProductById(UUID id) {
                Product product = productRepository.findWithDetailsById(id)
                                .orElseThrow(() -> new RuntimeException("Product not found"));
                return toProductDTOs(List.of(product)).get(0);
        }

        private List<ProductDTO> toProductDTOs(List<Product> products) {
                if (products.isEmpty())
                        return Collections.emptyList();

                List<UUID> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
                List<UUID> sellerIds = products.stream().map(p -> p.getSeller().getId()).distinct()
                                .collect(Collectors.toList());

                // 1. 이미지 일괄 조회
                Map<UUID, List<ProductImage>> imageMap = productImageRepository.findByProductIdIn(productIds)
                                .stream()
                                .collect(Collectors.groupingBy(img -> img.getProduct().getId()));

                // 2. 판매자 프로필 일괄 조회
                Map<UUID, BusinessProfile> profileMap = businessProfileRepository.findByUserIdIn(sellerIds)
                                .stream()
                                .filter(BusinessProfile::isMain)
                                .collect(Collectors.toMap(
                                                bp -> bp.getUser().getId(),
                                                bp -> bp,
                                                (p1, p2) -> p1));

                // 3. 매핑
                return products.stream().map(p -> {
                        List<String> images = imageMap.getOrDefault(p.getId(), Collections.emptyList())
                                        .stream()
                                        .sorted(Comparator.comparingInt(ProductImage::getDisplayOrder))
                                        .map(ProductImage::getImageUrl)
                                        .collect(Collectors.toList());

                        BusinessProfile profile = profileMap.get(p.getSeller().getId());
                        String sellerName = (profile != null) ? profile.getBusinessName() : p.getSeller().getName();

                        return productMapper.toDTO(p, sellerName, images);
                }).collect(Collectors.toList());
        }
}
