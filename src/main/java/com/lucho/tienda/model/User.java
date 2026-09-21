package com.lucho.tienda.model;

import com.lucho.tienda.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;

        // Safely compares natural business key (username) handling nulls natively
        return Objects.equals(username, user.getUsername());
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : getClass().hashCode();
    }
}