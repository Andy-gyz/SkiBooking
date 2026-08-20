package com.skibooking.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.dto.booking.BookingItemResponse;
import com.skibooking.dto.booking.BookingResponse;
import com.skibooking.dto.booking.BookingSummaryResponse;
import com.skibooking.dto.booking.CreateBookingRequest;
import com.skibooking.entity.Booking;
import com.skibooking.entity.BookingItem;
import com.skibooking.entity.Cart;
import com.skibooking.entity.CartItem;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.User;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.CartStatus;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.exception.AuthenticatedUserNotFoundException;
import com.skibooking.exception.BookingNotFoundException;
import com.skibooking.exception.CartNotFoundException;
import com.skibooking.exception.EmptyCartException;
import com.skibooking.exception.InsufficientLessonCapacityException;
import com.skibooking.exception.InvalidCheckoutException;
import com.skibooking.repository.BookingItemRepository;
import com.skibooking.repository.BookingRepository;
import com.skibooking.repository.CartItemRepository;
import com.skibooking.repository.CartRepository;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.UserRepository;

@Service
public class BookingService {

    private static final DateTimeFormatter BOOKING_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final LessonSessionRepository lessonSessionRepository;
    private final UserRepository userRepository;
    private final Clock clock = Clock.systemDefaultZone();

    public BookingService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            LessonSessionRepository lessonSessionRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.lessonSessionRepository = lessonSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(Jwt jwt, CreateBookingRequest request) {
        User user = findAuthenticatedUser(jwt);
        Cart cart = cartRepository.findByIdAndStatusForUpdate(request.cartId(), CartStatus.ACTIVE)
                .filter(candidate -> candidate.getUser() != null)
                .filter(candidate -> candidate.getUser().getId().equals(user.getId()))
                .orElseThrow(CartNotFoundException::new);
        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        Booking booking = new Booking();
        booking.setBookingNumber(generateBookingNumber());
        booking.setUser(user);
        booking.setCustomerFirstName(request.firstName().trim());
        booking.setCustomerLastName(request.lastName().trim());
        booking.setCustomerEmail(request.email().trim().toLowerCase(Locale.ROOT));
        booking.setCustomerPhone(normalizeOptional(request.phone()));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCurrency("AUD");

        List<BookingItem> bookingItems = cartItems.stream()
                .map(item -> createBookingItem(booking, item))
                .toList();
        BigDecimal total = bookingItems.stream()
                .map(BookingItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        booking.setTotalAmount(total);

        bookingRepository.saveAndFlush(booking);
        bookingItemRepository.saveAllAndFlush(bookingItems);
        cart.setStatus(CartStatus.CHECKED_OUT);
        cart.touch();
        cartRepository.save(cart);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Jwt jwt, String bookingNumber) {
        long userId = authenticatedUserId(jwt);
        Booking booking = bookingRepository.findByBookingNumberAndUserId(bookingNumber, userId)
                .orElseThrow(BookingNotFoundException::new);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> listMyBookings(Jwt jwt) {
        long userId = authenticatedUserId(jwt);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(booking -> BookingSummaryResponse.from(
                        booking,
                        bookingItemRepository.findByBookingIdOrderByIdAsc(booking.getId()).stream()
                                .mapToInt(BookingItem::getQuantity)
                                .sum()))
                .toList();
    }

    private BookingItem createBookingItem(Booking booking, CartItem cartItem) {
        Product product = productRepository.findByIdAndActiveTrueAndResort_Status(
                        cartItem.getProduct().getId(), ResortStatus.ACTIVE)
                .orElseThrow(() -> new InvalidCheckoutException(
                        "A product in the cart is no longer available."));
        validateQuantity(cartItem);

        LessonSession lockedSession = switch (product.getCategory()) {
            case RESORT_ACCESS -> {
                validateResortAccess(cartItem);
                yield null;
            }
            case LIFT_TICKET -> {
                validateFutureDate(cartItem.getBookingDate(), "Lift ticket booking date");
                yield null;
            }
            case LESSON -> validateAndReserveLesson(cartItem, product);
            case RENTAL -> {
                validateRental(cartItem);
                yield null;
            }
        };

        BookingItem item = new BookingItem();
        item.setBooking(booking);
        item.setProduct(product);
        item.setLessonSession(lockedSession);
        item.setProductName(product.getName());
        item.setCategory(product.getCategory());
        item.setQuantity(cartItem.getQuantity());
        item.setUnitPrice(product.getPrice());
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        item.setBookingDate(cartItem.getBookingDate());
        item.setVehicleRegistration(cartItem.getVehicleRegistration());
        item.setVehicleType(cartItem.getVehicleType());
        item.setEntryDate(cartItem.getEntryDate());
        item.setExitDate(cartItem.getExitDate());
        item.setRentalStartDate(cartItem.getRentalStartDate());
        item.setRentalEndDate(cartItem.getRentalEndDate());
        item.setRentalSize(cartItem.getRentalSize());
        item.setRentalBootSize(cartItem.getRentalBootSize());
        return item;
    }

    private LessonSession validateAndReserveLesson(CartItem cartItem, Product product) {
        if (cartItem.getLessonSession() == null) {
            throw new InvalidCheckoutException("A lesson item is missing its lesson session.");
        }
        LessonSession session = lessonSessionRepository.findByIdForUpdate(cartItem.getLessonSession().getId())
                .filter(candidate -> candidate.getStatus() == LessonSessionStatus.ACTIVE)
                .filter(candidate -> candidate.getProduct().getId().equals(product.getId()))
                .orElseThrow(() -> new InvalidCheckoutException(
                        "A lesson session in the cart is no longer available."));
        validateFutureDate(session.getSessionDate(), "Lesson session date");
        if (session.getAvailableCount() < cartItem.getQuantity()) {
            throw new InsufficientLessonCapacityException();
        }
        session.setBookedCount(session.getBookedCount() + cartItem.getQuantity());
        return session;
    }

    private void validateResortAccess(CartItem item) {
        if (!hasText(item.getVehicleRegistration()) || !hasText(item.getVehicleType())) {
            throw new InvalidCheckoutException("Resort access vehicle details are incomplete.");
        }
        validateFutureDate(item.getEntryDate(), "Resort entry date");
        requireOrderedDates(item.getEntryDate(), item.getExitDate(), "Resort access dates are invalid.");
    }

    private void validateRental(CartItem item) {
        if (!hasText(item.getRentalSize())) {
            throw new InvalidCheckoutException("Rental size is required.");
        }
        validateFutureDate(item.getRentalStartDate(), "Rental start date");
        requireOrderedDates(item.getRentalStartDate(), item.getRentalEndDate(), "Rental dates are invalid.");
    }

    private void validateQuantity(CartItem item) {
        if (item.getQuantity() < 1 || item.getQuantity() > 20) {
            throw new InvalidCheckoutException("Cart item quantity must be between 1 and 20.");
        }
    }

    private void validateFutureDate(LocalDate date, String field) {
        if (date == null || date.isBefore(LocalDate.now(clock))) {
            throw new InvalidCheckoutException(field + " must be today or later.");
        }
    }

    private void requireOrderedDates(LocalDate start, LocalDate end, String message) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new InvalidCheckoutException(message);
        }
    }

    private BookingResponse toResponse(Booking booking) {
        List<BookingItemResponse> items = bookingItemRepository
                .findByBookingIdOrderByIdAsc(booking.getId())
                .stream()
                .map(BookingItemResponse::from)
                .toList();
        return BookingResponse.from(booking, items);
    }

    private User findAuthenticatedUser(Jwt jwt) {
        return userRepository.findById(authenticatedUserId(jwt))
                .orElseThrow(AuthenticatedUserNotFoundException::new);
    }

    private long authenticatedUserId(Jwt jwt) {
        Number userId = jwt.getClaim("uid");
        if (userId == null) {
            throw new AuthenticatedUserNotFoundException();
        }
        return userId.longValue();
    }

    private String generateBookingNumber() {
        String date = LocalDate.now(clock).format(BOOKING_DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
        return "SKI-" + date + "-" + randomPart;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
