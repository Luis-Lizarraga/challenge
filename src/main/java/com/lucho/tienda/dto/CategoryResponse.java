package com.lucho.tienda.dto;

import com.lucho.tienda.model.Category;

public record CategoryResponse(
        Long id,
        String name
) {
    public static CategoryResponse fromEntity(Category category) {
        if (category == null) return null;

        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}