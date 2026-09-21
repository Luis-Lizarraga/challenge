package com.lucho.tienda.repository;

import com.lucho.tienda.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Basic CRUD operations provided by JpaRepository are enough
    // since categories are read-only and managed via SQL scripts
}