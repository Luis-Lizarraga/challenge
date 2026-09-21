package com.lucho.tienda.dto;

import com.lucho.tienda.model.Product;
import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String code,
        String name,
        CategoryResponse category,
        BigDecimal price,
        Integer stock
) {
    public static ProductResponse fromEntity(Product product) {
        if (product == null) return null;

        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                CategoryResponse.fromEntity(product.getCategory()),
                product.getPrice(),
                product.getStock()
        );
    }

    public static List<ProductResponse> fromEntityList(List<Product> products) {
        if (products == null) return List.of();

        return products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }
}