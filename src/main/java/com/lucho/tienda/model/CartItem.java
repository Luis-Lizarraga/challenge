package com.lucho.tienda.model;

import com.lucho.tienda.constant.CartConstants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "carrito_items", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_cart_items_cart_product",
                columnNames = {"carrito_id", "producto_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Product product;

    @Column(name = "cantidad", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // Frozen list price per unit captured at checkout time
    @Column(name = "precio_unitario")
    private BigDecimal unitPrice;

    // Frozen discount amount per unit captured at checkout time
    @Column(name = "monto_descuento")
    private BigDecimal discountAmount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem cartItem)) return false;

        // Entity identity is stable only after persistence. Two transient entities
        // are intentionally different objects; business uniqueness is enforced by
        // the cart service and the DB unique constraint (cart_id, product_id).
        return id != null && Objects.equals(id, cartItem.id);
    }

    @Override
    public int hashCode() {
        // Constant hash keeps hash-based collections stable when JPA assigns the ID.
        return getClass().hashCode();
    }

    public void changeQuantity(int newQuantity) {
        if (newQuantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        this.quantity = newQuantity;
    }

    public void increaseQuantity(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Quantity increment must be at least 1.");
        }
        changeQuantity(this.quantity + amount);
    }

    // Helper method to compute the net unit price after discount
    public BigDecimal getNetUnitPrice() {
        BigDecimal zeroScaled = BigDecimal.ZERO.setScale(CartConstants.MONEY_SCALE, RoundingMode.HALF_UP);

        if (unitPrice == null) {
            return zeroScaled;
        }

        BigDecimal discount = (discountAmount != null) ? discountAmount : zeroScaled;
        BigDecimal netPrice = unitPrice.subtract(discount);

        return netPrice.setScale(CartConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // Final subtotal charged for this line item
    public BigDecimal getLineTotal() {
        int effectiveQuantity = (quantity != null && quantity > 0)
                ? quantity
                : CartConstants.DEFAULT_QUANTITY;

        BigDecimal total = getNetUnitPrice().multiply(BigDecimal.valueOf(effectiveQuantity));

        return total.setScale(CartConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }
}