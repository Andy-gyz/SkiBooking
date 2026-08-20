package com.skibooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skibooking.entity.Product;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByResortIdAndName(Long resortId, String name);

    List<Product> findByCategoryAndActiveTrueOrderByNameAsc(ProductCategory category);

    List<Product> findByResortIdAndCategoryAndActiveTrueOrderByNameAsc(
            Long resortId,
            ProductCategory category);

    List<Product> findByActiveTrueAndResort_StatusOrderByNameAsc(ResortStatus resortStatus);

    List<Product> findByCategoryAndActiveTrueAndResort_StatusOrderByNameAsc(
            ProductCategory category,
            ResortStatus resortStatus);

    Optional<Product> findByIdAndActiveTrueAndResort_Status(Long id, ResortStatus resortStatus);
}
