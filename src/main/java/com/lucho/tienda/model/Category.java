package com.lucho.tienda.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;

        // Safely compares natural business key (code) handling nulls natively
        return Objects.equals(name, category.getName());
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : getClass().hashCode();
    }
}