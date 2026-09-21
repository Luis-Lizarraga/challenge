package com.lucho.tienda;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordGeneratorTest {

    @Test
    void generarBCrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password123";
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("=================================================");
        System.out.println("YOUR FRESH HASH FOR DATA.SQL:");
        System.out.println(encodedPassword);
        System.out.println("=================================================");
    }
}