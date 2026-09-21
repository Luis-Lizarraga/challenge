package com.lucho.tienda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // Dedicated configuration for Spring Retry interceptors
}