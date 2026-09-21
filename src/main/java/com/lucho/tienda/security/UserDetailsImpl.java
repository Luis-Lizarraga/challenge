package com.lucho.tienda.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lucho.tienda.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    // Static factory method to build the object from the User entity
    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

}