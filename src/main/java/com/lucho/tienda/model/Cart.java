package com.lucho.tienda.model;

import com.lucho.tienda.model.enums.CartStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carritos", indexes = {
        @Index(name = "idx_carritos_usuario", columnList = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CartItem> items = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private CartStatus status = CartStatus.CREATED;

    @Column(name = "monto_total", nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Version
    private Long version;

    /**
     * Calculates the total from the line-item snapshot prices.
     * Pricing rules belong to CartItem; Cart only aggregates line totals.
     */
    public BigDecimal calculateTotal() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .filter(java.util.Objects::nonNull)
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void recalculateTotal() {
        this.totalAmount = calculateTotal();
    }

    /**
     * Starts checkout. State transitions belong to the aggregate instead of
     * being scattered across services.
     */
    public void startProcessing() {
        status.validateForCheckout();
        if (items == null || items.isEmpty()) {
            throw new com.lucho.tienda.exception.BadRequestException(
                    "Cannot initiate checkout for an empty cart.");
        }
        this.status = CartStatus.PROCESSING;
    }

    /** Marks a successfully processed cart as completed. */
    public void markAsProcessed() {
        if (status != CartStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only a PROCESSING cart can be marked as PROCESSED.");
        }
        this.status = CartStatus.PROCESSED;
    }

    public void addItem(CartItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cart item is required.");
        }
        this.items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        if (item == null) {
            return;
        }
        if (this.items.remove(item)) {
            item.setCart(null);
        }
    }
}
