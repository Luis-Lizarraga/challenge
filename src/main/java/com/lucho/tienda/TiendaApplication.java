package com.lucho.tienda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Enables the use of background threads
public class TiendaApplication {

	public static void main(String[] args) {
		SpringApplication.run(TiendaApplication.class, args);
	}

}