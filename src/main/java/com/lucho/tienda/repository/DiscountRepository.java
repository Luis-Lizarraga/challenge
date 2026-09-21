package com.lucho.tienda.repository;

import com.lucho.tienda.model.Discount;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    @Cacheable("activeDiscounts")
    List<Discount> findByActiveTrue();
}