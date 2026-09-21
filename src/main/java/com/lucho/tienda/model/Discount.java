package com.lucho.tienda.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "descuentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Category category;

    @Column(name = "porcentaje", nullable = false)
    @Builder.Default
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.FALSE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Discount discount)) return false;

        // Use getters to safely initialize Hibernate LAZY proxies if needed.
        // This ensures a null-safe comparison even if the entity is not fully loaded.
        return this.getCategory() != null && discount.getCategory() != null &&
                Objects.equals(this.getCategory().getId(), discount.getCategory().getId());
    }

    @Override
    public int hashCode() {
        // Delegate hashCode to the class level to maintain consistency across
        // entity lifecycle states (transient vs persisted) and avoid dropping
        // entities from Sets/HashMaps.
        return getClass().hashCode();
    }
}