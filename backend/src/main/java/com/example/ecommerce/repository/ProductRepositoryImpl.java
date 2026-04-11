package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.QProduct;
import com.example.ecommerce.entity.QCategory;
import com.example.ecommerce.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> searchProducts(UUID categoryId, String itemCondition, String itemName) {
        QProduct product = QProduct.product;
        QCategory category = QCategory.category;
        QUser seller = QUser.user;

        return queryFactory
                .selectFrom(product)
                .leftJoin(product.category, category).fetchJoin()
                .leftJoin(product.seller, seller).fetchJoin()
                .where(
                        product.isDisplayed.isTrue(),
                        categoryIdEq(categoryId),
                        itemConditionEq(itemCondition),
                        itemNameContains(itemName)
                )
                .fetch();
    }

    private BooleanExpression categoryIdEq(UUID categoryId) {
        return categoryId != null ? QProduct.product.category.id.eq(categoryId) : null;
    }

    private BooleanExpression itemConditionEq(String itemCondition) {
        return StringUtils.hasText(itemCondition) ? QProduct.product.itemCondition.eq(itemCondition) : null;
    }

    private BooleanExpression itemNameContains(String itemName) {
        return StringUtils.hasText(itemName) ? QProduct.product.itemName.containsIgnoreCase(itemName) : null;
    }
}
