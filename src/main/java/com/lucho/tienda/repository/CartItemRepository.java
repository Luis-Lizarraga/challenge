package com.lucho.tienda.repository;

import com.lucho.tienda.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Hereda los métodos CRUD estándar y nuestro método upsertCartItemAtomic
}