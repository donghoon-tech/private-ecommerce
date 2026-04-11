package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Program;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductRepositoryCustom {
    @EntityGraph(attributePaths = {"category", "seller"})
    Optional<Product> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Product> findBySellerId(UUID sellerId);

    List<Product> findByCategoryId(UUID categoryId);

    List<Product> findByStatus(Product.Status status);

    long countBySellerId(UUID sellerId);
}