package com.lucho.tienda.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "nombre", nullable = false)
    private String name;

    @Column(name = "precio", nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;

        // Safely compares natural business key (code) handling nulls natively
        return Objects.equals(code, product.getCode());
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : getClass().hashCode();
    }
}