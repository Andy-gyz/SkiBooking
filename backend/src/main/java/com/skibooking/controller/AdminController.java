package com.skibooking.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skibooking.dto.admin.AdminBookingDetailResponse;
import com.skibooking.dto.admin.AdminDashboardResponse;
import com.skibooking.dto.admin.AdminLessonSessionRequest;
import com.skibooking.dto.admin.AdminLessonSessionResponse;
import com.skibooking.dto.admin.AdminProductRequest;
import com.skibooking.dto.admin.AdminProductResponse;
import com.skibooking.dto.admin.AdminReservationResponse;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    AdminDashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/bookings")
    List<AdminReservationResponse> listReservations(
            @RequestParam ProductCategory category) {
        return adminService.listReservations(category);
    }

    @GetMapping("/bookings/{id}")
    AdminBookingDetailResponse bookingDetail(@PathVariable Long id) {
        return adminService.bookingDetail(id);
    }

    @GetMapping("/products")
    List<AdminProductResponse> listProducts(
            @RequestParam(required = false) ProductCategory category) {
        return adminService.listProducts(category);
    }

    @PostMapping("/products")
    ResponseEntity<AdminProductResponse> createProduct(
            @Valid @RequestBody AdminProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    AdminProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody AdminProductRequest request) {
        return adminService.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        adminService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lesson-sessions")
    List<AdminLessonSessionResponse> listLessonSessions(
            @RequestParam(required = false) Long productId) {
        return adminService.listLessonSessions(productId);
    }

    @PostMapping("/lesson-sessions")
    ResponseEntity<AdminLessonSessionResponse> createLessonSession(
            @Valid @RequestBody AdminLessonSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createLessonSession(request));
    }

    @PutMapping("/lesson-sessions/{id}")
    AdminLessonSessionResponse updateLessonSession(
            @PathVariable Long id,
            @Valid @RequestBody AdminLessonSessionRequest request) {
        return adminService.updateLessonSession(id, request);
    }
}
