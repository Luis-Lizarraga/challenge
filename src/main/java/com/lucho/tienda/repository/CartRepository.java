package com.lucho.tienda.repository;

import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.enums.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Cart> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    // Internal/system lookup used by background order processing. User-facing flows use user-scoped methods below.
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    Optional<Cart> findCartById(Long id);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    @Query("SELECT c FROM Cart c WHERE c.id = :cartId AND c.user.id = :userId")
    Optional<Cart> findByIdAndUserId(@Param("cartId") Long cartId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.id = :cartId AND c.user.id = :userId")
    Optional<Cart> findByIdAndUserIdForUpdate(@Param("cartId") Long cartId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Cart c SET c.status = :status WHERE c.id = :id")
    void updateCartStatus(@Param("id") Long id, @Param("status") CartStatus status);
}
