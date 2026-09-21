package com.lucho.tienda.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching // <-- it actives the cache support
public class CacheConfig {
}