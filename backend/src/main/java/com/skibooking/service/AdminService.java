package com.skibooking.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.dto.admin.AdminBookingDetailResponse;
import com.skibooking.dto.admin.AdminDashboardResponse;
import com.skibooking.dto.admin.AdminLessonSessionRequest;
import com.skibooking.dto.admin.AdminLessonSessionResponse;
import com.skibooking.dto.admin.AdminPaymentResponse;
import com.skibooking.dto.admin.AdminProductRequest;
import com.skibooking.dto.admin.AdminProductResponse;
import com.skibooking.dto.admin.AdminReservationResponse;
import com.skibooking.dto.booking.BookingItemResponse;
import com.skibooking.entity.Booking;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.exception.InvalidAdminRequestException;
import com.skibooking.exception.ResourceNotFoundException;
import com.skibooking.repository.BookingItemRepository;
import com.skibooking.repository.BookingRepository;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.PaymentRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;

@Service
public class AdminService {

    private static final EnumSet<BookingStatus> ACTIVE_RESERVATION_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final PaymentRepository paymentRepository;
    private final ResortRepository resortRepository;
    private final ProductRepository productRepository;
    private final LessonSessionRepository lessonSessionRepository;

    public AdminService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            PaymentRepository paymentRepository,
            ResortRepository resortRepository,
            ProductRepository productRepository,
            LessonSessionRepository lessonSessionRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.paymentRepository = paymentRepository;
        this.resortRepository = resortRepository;
        this.productRepository = productRepository;
        this.lessonSessionRepository = lessonSessionRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                reservationCount(ProductCategory.RESORT_ACCESS),
                reservationCount(ProductCategory.LIFT_TICKET),
                reservationCount(ProductCategory.LESSON),
                reservationCount(ProductCategory.RENTAL));
    }

    @Transactional(readOnly = true)
    public List<AdminReservationResponse> listReservations(ProductCategory category) {
        return bookingItemRepository.findAdminReservations(category, ACTIVE_RESERVATION_STATUSES).stream()
                .map(item -> AdminReservationResponse.from(
                        item,
                        paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(item.getBooking().getId())
                                .orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminBookingDetailResponse bookingDetail(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        List<BookingItemResponse> items = bookingItemRepository.findByBookingIdOrderByIdAsc(id).stream()
                .map(BookingItemResponse::from)
                .toList();
        List<AdminPaymentResponse> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(id).stream()
                .map(AdminPaymentResponse::from)
                .toList();
        return AdminBookingDetailResponse.from(booking, items, payments);
    }

    @Transactional(readOnly = true)
    public List<AdminProductResponse> listProducts(ProductCategory category) {
        List<Product> products = category == null
                ? productRepository.findAllByOrderByNameAsc()
                : productRepository.findByCategoryOrderByNameAsc(category);
        return products.stream().map(AdminProductResponse::from).toList();
    }

    @Transactional
    public AdminProductResponse createProduct(AdminProductRequest request) {
        Resort resort = findResort(request.resortId());
        String name = request.name().trim();
        ensureUniqueProductName(resort.getId(), name, null);

        Product product = new Product();
        applyProduct(product, resort, request, name);
        return AdminProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Transactional
    public AdminProductResponse updateProduct(Long id, AdminProductRequest request) {
        Product product = findProduct(id);
        if (product.getCategory() != request.category()
                && lessonSessionRepository.existsByProductId(product.getId())) {
            throw new InvalidAdminRequestException(
                    "A product with lesson sessions cannot change category.");
        }
        Resort resort = findResort(request.resortId());
        String name = request.name().trim();
        ensureUniqueProductName(resort.getId(), name, product.getId());
        applyProduct(product, resort, request, name);
        return AdminProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = findProduct(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<AdminLessonSessionResponse> listLessonSessions(Long productId) {
        List<LessonSession> sessions = productId == null
                ? lessonSessionRepository.findAllByOrderBySessionDateAscStartTimeAsc()
                : lessonSessionRepository.findByProductIdOrderBySessionDateAscStartTimeAsc(productId);
        return sessions.stream().map(AdminLessonSessionResponse::from).toList();
    }

    @Transactional
    public AdminLessonSessionResponse createLessonSession(AdminLessonSessionRequest request) {
        Product product = findLessonProduct(request.productId());
        validateSessionRequest(request);
        if (lessonSessionRepository.existsByProductIdAndSessionDateAndStartTime(
                product.getId(), request.date(), request.startTime())) {
            throw new InvalidAdminRequestException("A lesson session already exists in this time slot.");
        }

        LessonSession session = new LessonSession();
        session.setProduct(product);
        session.setSessionDate(request.date());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setCapacity(request.capacity());
        session.setBookedCount(0);
        session.setStatus(request.status());
        return AdminLessonSessionResponse.from(lessonSessionRepository.saveAndFlush(session));
    }

    @Transactional
    public AdminLessonSessionResponse updateLessonSession(
            Long id,
            AdminLessonSessionRequest request) {
        LessonSession session = lessonSessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson session", id));
        Product product = findLessonProduct(request.productId());
        validateSessionRequest(request);
        if (request.capacity() < session.getBookedCount()) {
            throw new InvalidAdminRequestException(
                    "Lesson capacity cannot be lower than the current booked count.");
        }
        if (session.getBookedCount() > 0 && schedulingDetailsChanged(session, product, request)) {
            throw new InvalidAdminRequestException(
                    "A lesson session with bookings cannot be moved, reassigned, or cancelled.");
        }
        if (lessonSessionRepository.existsByProductIdAndSessionDateAndStartTimeAndIdNot(
                product.getId(), request.date(), request.startTime(), id)) {
            throw new InvalidAdminRequestException("A lesson session already exists in this time slot.");
        }

        session.setProduct(product);
        session.setSessionDate(request.date());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setCapacity(request.capacity());
        session.setStatus(request.status());
        return AdminLessonSessionResponse.from(lessonSessionRepository.saveAndFlush(session));
    }

    private long reservationCount(ProductCategory category) {
        return bookingItemRepository.countReservedQuantity(category, ACTIVE_RESERVATION_STATUSES);
    }

    private void applyProduct(
            Product product,
            Resort resort,
            AdminProductRequest request,
            String normalizedName) {
        product.setResort(resort);
        product.setName(normalizedName);
        product.setCategory(request.category());
        product.setDescription(normalizeOptional(request.description()));
        product.setPrice(request.price());
        product.setImageUrl(normalizeOptional(request.imageUrl()));
        product.setActive(request.active());
    }

    private void ensureUniqueProductName(Long resortId, String name, Long currentProductId) {
        productRepository.findByResortIdAndName(resortId, name).ifPresent(existing -> {
            if (currentProductId == null || !existing.getId().equals(currentProductId)) {
                throw new InvalidAdminRequestException(
                        "A product with this name already exists at the resort.");
            }
        });
    }

    private void validateSessionRequest(AdminLessonSessionRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidAdminRequestException("Lesson end time must be after start time.");
        }
    }

    private boolean schedulingDetailsChanged(
            LessonSession session,
            Product product,
            AdminLessonSessionRequest request) {
        return !session.getProduct().getId().equals(product.getId())
                || !session.getSessionDate().equals(request.date())
                || !session.getStartTime().equals(request.startTime())
                || !session.getEndTime().equals(request.endTime())
                || request.status() == LessonSessionStatus.CANCELLED;
    }

    private Resort findResort(Long id) {
        return resortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resort", id));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Product findLessonProduct(Long id) {
        Product product = findProduct(id);
        if (product.getCategory() != ProductCategory.LESSON) {
            throw new InvalidAdminRequestException(
                    "Lesson sessions can only belong to a LESSON product.");
        }
        return product;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
