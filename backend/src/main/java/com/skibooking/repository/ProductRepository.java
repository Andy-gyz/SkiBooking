package com.skibooking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skibooking.entity.Product;
import com.skibooking.entity.enums.ProductCategory;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByResortIdAndName(Long resortId, String name);

    List<Product> findByCategoryAndActiveTrueOrderByNameAsc(ProductCategory category);

    List<Product> findByResortIdAndCategoryAndActiveTrueOrderByNameAsc(
            Long resortId,
            ProductCategory category);
}

