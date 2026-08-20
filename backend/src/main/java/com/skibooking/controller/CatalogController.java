package com.skibooking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skibooking.dto.product.LessonSessionResponse;
import com.skibooking.dto.product.ProductResponse;
import com.skibooking.dto.product.ResortResponse;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.service.CatalogService;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/resorts")
    List<ResortResponse> listResorts() {
        return catalogService.listResorts();
    }

    @GetMapping("/resorts/{id}")
    ResortResponse getResort(@PathVariable Long id) {
        return catalogService.getResort(id);
    }

    @GetMapping("/products")
    List<ProductResponse> listProducts(
            @RequestParam(required = false) ProductCategory category) {
        return catalogService.listProducts(category);
    }

    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable Long id) {
        return catalogService.getProduct(id);
    }

    @GetMapping("/lesson-sessions")
    List<LessonSessionResponse> listLessonSessions(
            @RequestParam Long productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return catalogService.listLessonSessions(productId, date);
    }
}
