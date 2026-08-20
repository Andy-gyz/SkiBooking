package com.skibooking.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.dto.product.LessonSessionResponse;
import com.skibooking.dto.product.ProductResponse;
import com.skibooking.dto.product.ResortResponse;
import com.skibooking.entity.Product;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.exception.InvalidCatalogRequestException;
import com.skibooking.exception.ResourceNotFoundException;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ResortRepository resortRepository;
    private final ProductRepository productRepository;
    private final LessonSessionRepository lessonSessionRepository;

    public CatalogService(
            ResortRepository resortRepository,
            ProductRepository productRepository,
            LessonSessionRepository lessonSessionRepository) {
        this.resortRepository = resortRepository;
        this.productRepository = productRepository;
        this.lessonSessionRepository = lessonSessionRepository;
    }

    public List<ResortResponse> listResorts() {
        return resortRepository.findByStatusOrderByNameAsc(ResortStatus.ACTIVE).stream()
                .map(ResortResponse::from)
                .toList();
    }

    public ResortResponse getResort(Long id) {
        return resortRepository.findByIdAndStatus(id, ResortStatus.ACTIVE)
                .map(ResortResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Resort", id));
    }

    public List<ProductResponse> listProducts(ProductCategory category) {
        List<Product> products = category == null
                ? productRepository.findByActiveTrueAndResort_StatusOrderByNameAsc(ResortStatus.ACTIVE)
                : productRepository.findByCategoryAndActiveTrueAndResort_StatusOrderByNameAsc(
                        category,
                        ResortStatus.ACTIVE);
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findAvailableProduct(id));
    }

    public List<LessonSessionResponse> listLessonSessions(Long productId, LocalDate date) {
        Product product = findAvailableProduct(productId);
        if (product.getCategory() != ProductCategory.LESSON) {
            throw new InvalidCatalogRequestException(
                    "Lesson sessions can only be requested for a LESSON product.");
        }

        return lessonSessionRepository
                .findByProductIdAndSessionDateAndStatusOrderByStartTimeAsc(
                        productId,
                        date,
                        LessonSessionStatus.ACTIVE)
                .stream()
                .map(LessonSessionResponse::from)
                .toList();
    }

    private Product findAvailableProduct(Long id) {
        return productRepository.findByIdAndActiveTrueAndResort_Status(id, ResortStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
