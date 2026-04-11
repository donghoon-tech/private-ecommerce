package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductDTO;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductImage;
import com.example.ecommerce.repository.BusinessProfileRepository;
import com.example.ecommerce.repository.ProductImageRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
        private final ProductRepository productRepository;
        private final ProductImageRepository productImageRepository;
        private final BusinessProfileRepository businessProfileRepository;
        private final com.example.ecommerce.mapper.ProductMapper productMapper;

        public List<ProductDTO> getAllProducts() {
                List<Product> products = productRepository.searchProducts(null, null, null);
                return toProductDTOs(products);
        }

        public List<ProductDTO> getProductsBySeller(UUID sellerId) {
                List<Product> products = productRepository.findBySellerId(sellerId);
                return toProductDTOs(products);
        }

        public List<ProductDTO> searchProducts(UUID categoryId, String itemCondition, String itemName) {
                List<Product> products = productRepository.searchProducts(categoryId, itemCondition, itemName);
                return toProductDTOs(products);
        }

        public ProductDTO getProductById(UUID id) {
                Product product = productRepository.findWithDetailsById(id)
                                .orElseThrow(() -> new RuntimeException("Product not found"));
                return toProductDTOs(List.of(product)).get(0);
        }

        private List<ProductDTO> toProductDTOs(List<Product> products) {
                if (products.isEmpty())
                        return java.util.Collections.emptyList();

                List<UUID> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
                List<UUID> sellerIds = products.stream().map(p -> p.getSeller().getId()).distinct()
                                .collect(Collectors.toList());

                // 1. 이미지 일괄 조회
                java.util.Map<UUID, List<ProductImage>> imageMap = productImageRepository.findByProductIdIn(productIds)
                                .stream()
                                .collect(Collectors.groupingBy(img -> img.getProduct().getId()));

                // 2. 판매자 프로필 일괄 조회
                java.util.Map<UUID, BusinessProfile> profileMap = businessProfileRepository.findByUserIdIn(sellerIds)
                                .stream()
                                .filter(BusinessProfile::isMain)
                                .collect(Collectors.toMap(
                                                bp -> bp.getUser().getId(),
                                                bp -> bp,
                                                (p1, p2) -> p1));

                // 3. 매핑
                return products.stream().map(p -> {
                        List<String> images = imageMap.getOrDefault(p.getId(), java.util.Collections.emptyList())
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
