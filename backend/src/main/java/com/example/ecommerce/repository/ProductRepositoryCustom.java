package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import java.util.List;
import java.util.UUID;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(UUID categoryId, String itemCondition, String itemName);
}
