package com.lucho.tienda.repository;

import com.lucho.tienda.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>{
    Optional<Product> findByCode(String code);
    /**
     * Atomically decrements the stock only if there is enough available.
     * Returns the number of affected rows (1 if successful, 0 if out of stock).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.code = :code AND p.stock >= :quantity")
    int decrementStockSafely(@Param("code") String code, @Param("quantity") int quantity);
}
