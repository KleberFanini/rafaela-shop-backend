package com.rafaelagamero.backend.repository;

import com.rafaelagamero.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.active IS NULL OR p.active = true")
    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (p.active IS NULL OR p.active = true)")
    List<Product> findByCategoryIdAndActiveTrue(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active IS NULL OR p.active = true")
    long countByActiveTrue();
}