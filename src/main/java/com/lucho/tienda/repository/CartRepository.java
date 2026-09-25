package com.lucho.tienda.repository;

import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.enums.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "items.product.category"
    })
    List<Cart> findByUserId(Long userId);

    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "items.product.category"
    })
    List<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    /**
     * Internal/system lookup used by background order processing.
     * Loads all associations required by the order processing flow.
     */
    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "items.product.category"
    })
    Optional<Cart> findCartById(Long id);

    /**
     * User-scoped cart lookup.
     * Loads the complete object graph required to build CartResponse
     * after the transactional service method has completed.
     */
    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "items.product.category"
    })
    @Query("""
            SELECT c
            FROM Cart c
            WHERE c.id = :cartId
              AND c.user.id = :userId
            """)
    Optional<Cart> findByIdAndUserId(
            @Param("cartId") Long cartId,
            @Param("userId") Long userId
    );

    /**
     * Locks the cart for write operations and loads the associations
     * required to build the response after the transaction completes.
     *
     * The pessimistic lock prevents concurrent modifications of the same cart.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM Cart c
            WHERE c.id = :cartId
              AND c.user.id = :userId
            """)
    Optional<Cart> findByIdAndUserIdForUpdate(
            @Param("cartId") Long cartId,
            @Param("userId") Long userId
    );
}