package com.skibooking.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.dto.cart.CartItemResponse;
import com.skibooking.dto.cart.CartResponse;
import com.skibooking.dto.cart.CreateCartItemRequest;
import com.skibooking.dto.cart.CreateCartResponse;
import com.skibooking.dto.cart.UpdateCartItemRequest;
import com.skibooking.entity.Cart;
import com.skibooking.entity.CartItem;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.User;
import com.skibooking.entity.enums.CartStatus;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.exception.AuthenticatedUserNotFoundException;
import com.skibooking.exception.CartItemNotFoundException;
import com.skibooking.exception.CartNotFoundException;
import com.skibooking.exception.InsufficientLessonCapacityException;
import com.skibooking.exception.InvalidCartItemException;
import com.skibooking.exception.ResourceNotFoundException;
import com.skibooking.repository.CartItemRepository;
import com.skibooking.repository.CartRepository;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.UserRepository;

@Service
public class CartService {

    private static final int CART_TOKEN_BYTES = 32;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final LessonSessionRepository lessonSessionRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            LessonSessionRepository lessonSessionRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.lessonSessionRepository = lessonSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateCartResponse createCart(Jwt jwt) {
        User user = jwt == null ? null : findAuthenticatedUser(jwt);
        if (user != null) {
            return cartRepository.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(
                            user.getId(), CartStatus.ACTIVE)
                    .map(cart -> new CreateCartResponse(null, toResponse(cart)))
                    .orElseGet(() -> createCartForUser(user));
        }

        String token = generateCartToken();
        Cart cart = new Cart();
        cart.setSessionToken(token);
        cart.setStatus(CartStatus.ACTIVE);
        cartRepository.saveAndFlush(cart);
        return new CreateCartResponse(token, toResponse(cart));
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long cartId, String cartToken, Jwt jwt) {
        Cart cart = cartRepository.findByIdAndStatus(cartId, CartStatus.ACTIVE)
                .orElseThrow(CartNotFoundException::new);
        verifyAccess(cart, cartToken, jwt);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(
            Long cartId,
            String cartToken,
            Jwt jwt,
            CreateCartItemRequest request) {
        Cart cart = findAccessibleCartForUpdate(cartId, cartToken, jwt);
        Product product = findAvailableProduct(request.productId());

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        applyConfiguration(item, product, CartItemInput.from(request));
        item.setUnitPrice(product.getPrice());
        cartItemRepository.save(item);
        cart.touch();
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(
            Long cartId,
            Long itemId,
            String cartToken,
            Jwt jwt,
            UpdateCartItemRequest request) {
        Cart cart = findAccessibleCartForUpdate(cartId, cartToken, jwt);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(CartItemNotFoundException::new);
        Product product = findAvailableProduct(item.getProduct().getId());

        applyConfiguration(item, product, CartItemInput.from(request));
        item.setUnitPrice(product.getPrice());
        cartItemRepository.save(item);
        cart.touch();
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public void deleteItem(Long cartId, Long itemId, String cartToken, Jwt jwt) {
        Cart cart = findAccessibleCartForUpdate(cartId, cartToken, jwt);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(CartItemNotFoundException::new);
        cartItemRepository.delete(item);
        cart.touch();
        cartRepository.save(cart);
    }

    @Transactional
    public Long attachAnonymousCart(String cartToken, User user) {
        Cart userCart = cartRepository.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(
                        user.getId(), CartStatus.ACTIVE)
                .orElse(null);
        if (cartToken == null || cartToken.isBlank()) {
            return userCart == null ? null : userCart.getId();
        }

        Cart anonymousCart = cartRepository.findBySessionTokenAndStatusForUpdate(
                        cartToken, CartStatus.ACTIVE)
                .filter(cart -> cart.getUser() == null)
                .orElseThrow(CartNotFoundException::new);

        if (userCart == null) {
            anonymousCart.setUser(user);
            anonymousCart.setSessionToken(null);
            anonymousCart.touch();
            return cartRepository.save(anonymousCart).getId();
        }

        List<CartItem> anonymousItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(
                anonymousCart.getId());
        anonymousItems.forEach(item -> item.setCart(userCart));
        cartItemRepository.saveAll(anonymousItems);

        anonymousCart.setUser(user);
        anonymousCart.setSessionToken(null);
        anonymousCart.setStatus(CartStatus.ABANDONED);
        anonymousCart.touch();
        userCart.touch();
        cartRepository.save(anonymousCart);
        cartRepository.save(userCart);
        return userCart.getId();
    }

    private CreateCartResponse createCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(CartStatus.ACTIVE);
        cartRepository.saveAndFlush(cart);
        return new CreateCartResponse(null, toResponse(cart));
    }

    private Cart findAccessibleCartForUpdate(Long cartId, String cartToken, Jwt jwt) {
        Cart cart = cartRepository.findByIdAndStatusForUpdate(cartId, CartStatus.ACTIVE)
                .orElseThrow(CartNotFoundException::new);
        verifyAccess(cart, cartToken, jwt);
        return cart;
    }

    private void verifyAccess(Cart cart, String cartToken, Jwt jwt) {
        if (cart.getUser() != null) {
            if (jwt == null || authenticatedUserId(jwt) != cart.getUser().getId()) {
                throw new CartNotFoundException();
            }
            return;
        }

        if (cartToken == null || !tokensMatch(cart.getSessionToken(), cartToken)) {
            throw new CartNotFoundException();
        }
    }

    private void applyConfiguration(CartItem item, Product product, CartItemInput input) {
        clearConfiguration(item);
        item.setQuantity(input.quantity());

        switch (product.getCategory()) {
            case RESORT_ACCESS -> applyResortAccess(item, input);
            case LIFT_TICKET -> applyLiftTicket(item, input);
            case LESSON -> applyLesson(item, product, input);
            case RENTAL -> applyRental(item, input);
        }
    }

    private void applyResortAccess(CartItem item, CartItemInput input) {
        rejectUnexpected(
                input.bookingDate() != null
                        || input.lessonSessionId() != null
                        || input.rentalStartDate() != null
                        || input.rentalEndDate() != null
                        || hasText(input.rentalSize())
                        || hasText(input.rentalBootSize()));
        item.setVehicleRegistration(requireText(input.vehicleRegistration(), "vehicleRegistration"));
        item.setVehicleType(requireText(input.vehicleType(), "vehicleType"));
        item.setEntryDate(requireDate(input.entryDate(), "entryDate"));
        item.setExitDate(requireDate(input.exitDate(), "exitDate"));
        requireOrderedDates(item.getEntryDate(), item.getExitDate(), "exitDate must not be before entryDate.");
    }

    private void applyLiftTicket(CartItem item, CartItemInput input) {
        rejectUnexpected(
                hasText(input.vehicleRegistration())
                        || hasText(input.vehicleType())
                        || input.entryDate() != null
                        || input.exitDate() != null
                        || input.lessonSessionId() != null
                        || input.rentalStartDate() != null
                        || input.rentalEndDate() != null
                        || hasText(input.rentalSize())
                        || hasText(input.rentalBootSize()));
        item.setBookingDate(requireDate(input.bookingDate(), "bookingDate"));
    }

    private void applyLesson(CartItem item, Product product, CartItemInput input) {
        rejectUnexpected(
                input.bookingDate() != null
                        || hasText(input.vehicleRegistration())
                        || hasText(input.vehicleType())
                        || input.entryDate() != null
                        || input.exitDate() != null
                        || input.rentalStartDate() != null
                        || input.rentalEndDate() != null
                        || hasText(input.rentalSize())
                        || hasText(input.rentalBootSize()));
        if (input.lessonSessionId() == null) {
            throw new InvalidCartItemException("lessonSessionId is required for a LESSON product.");
        }
        LessonSession session = lessonSessionRepository.findById(input.lessonSessionId())
                .filter(candidate -> candidate.getStatus() == LessonSessionStatus.ACTIVE)
                .filter(candidate -> candidate.getProduct().getId().equals(product.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Lesson session", input.lessonSessionId()));
        if (session.getAvailableCount() < input.quantity()) {
            throw new InsufficientLessonCapacityException();
        }
        item.setLessonSession(session);
        item.setBookingDate(session.getSessionDate());
    }

    private void applyRental(CartItem item, CartItemInput input) {
        rejectUnexpected(
                input.bookingDate() != null
                        || hasText(input.vehicleRegistration())
                        || hasText(input.vehicleType())
                        || input.entryDate() != null
                        || input.exitDate() != null
                        || input.lessonSessionId() != null);
        item.setRentalStartDate(requireDate(input.rentalStartDate(), "rentalStartDate"));
        item.setRentalEndDate(requireDate(input.rentalEndDate(), "rentalEndDate"));
        item.setRentalSize(requireText(input.rentalSize(), "rentalSize"));
        item.setRentalBootSize(normalizeOptional(input.rentalBootSize()));
        requireOrderedDates(
                item.getRentalStartDate(),
                item.getRentalEndDate(),
                "rentalEndDate must not be before rentalStartDate.");
    }

    private void clearConfiguration(CartItem item) {
        item.setLessonSession(null);
        item.setBookingDate(null);
        item.setVehicleRegistration(null);
        item.setVehicleType(null);
        item.setEntryDate(null);
        item.setExitDate(null);
        item.setRentalStartDate(null);
        item.setRentalEndDate(null);
        item.setRentalSize(null);
        item.setRentalBootSize(null);
    }

    private Product findAvailableProduct(Long productId) {
        return productRepository.findByIdAndActiveTrueAndResort_Status(productId, ResortStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
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

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId())
                .stream()
                .map(CartItemResponse::from)
                .toList();
        return CartResponse.from(cart, items);
    }

    private String generateCartToken() {
        byte[] bytes = new byte[CART_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean tokensMatch(String expected, String actual) {
        return expected != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidCartItemException(field + " is required for this product category.");
        }
        return value.trim();
    }

    private LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw new InvalidCartItemException(field + " is required for this product category.");
        }
        return value;
    }

    private void requireOrderedDates(LocalDate start, LocalDate end, String message) {
        if (end.isBefore(start)) {
            throw new InvalidCartItemException(message);
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void rejectUnexpected(boolean hasUnexpectedFields) {
        if (hasUnexpectedFields) {
            throw new InvalidCartItemException(
                    "The request contains fields that do not apply to this product category.");
        }
    }

    private record CartItemInput(
            int quantity,
            LocalDate bookingDate,
            String vehicleRegistration,
            String vehicleType,
            LocalDate entryDate,
            LocalDate exitDate,
            Long lessonSessionId,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate,
            String rentalSize,
            String rentalBootSize) {

        static CartItemInput from(CreateCartItemRequest request) {
            return new CartItemInput(
                    request.quantity(),
                    request.bookingDate(),
                    request.vehicleRegistration(),
                    request.vehicleType(),
                    request.entryDate(),
                    request.exitDate(),
                    request.lessonSessionId(),
                    request.rentalStartDate(),
                    request.rentalEndDate(),
                    request.rentalSize(),
                    request.rentalBootSize());
        }

        static CartItemInput from(UpdateCartItemRequest request) {
            return new CartItemInput(
                    request.quantity(),
                    request.bookingDate(),
                    request.vehicleRegistration(),
                    request.vehicleType(),
                    request.entryDate(),
                    request.exitDate(),
                    request.lessonSessionId(),
                    request.rentalStartDate(),
                    request.rentalEndDate(),
                    request.rentalSize(),
                    request.rentalBootSize());
        }
    }
}
