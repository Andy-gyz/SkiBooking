package com.skibooking.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skibooking.dto.cart.CartResponse;
import com.skibooking.dto.cart.CreateCartItemRequest;
import com.skibooking.dto.cart.CreateCartResponse;
import com.skibooking.dto.cart.UpdateCartItemRequest;
import com.skibooking.service.CartService;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private static final String CART_TOKEN_HEADER = "X-Cart-Token";

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    ResponseEntity<CreateCartResponse> createCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.createCart(jwt));
    }

    @GetMapping("/{cartId}")
    CartResponse getCart(
            @PathVariable Long cartId,
            @RequestHeader(name = CART_TOKEN_HEADER, required = false) String cartToken,
            @AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(cartId, cartToken, jwt);
    }

    @PostMapping("/{cartId}/items")
    ResponseEntity<CartResponse> addItem(
            @PathVariable Long cartId,
            @RequestHeader(name = CART_TOKEN_HEADER, required = false) String cartToken,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(cartId, cartToken, jwt, request));
    }

    @PutMapping("/{cartId}/items/{itemId}")
    CartResponse updateItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId,
            @RequestHeader(name = CART_TOKEN_HEADER, required = false) String cartToken,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(cartId, itemId, cartToken, jwt, request);
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    ResponseEntity<Void> deleteItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId,
            @RequestHeader(name = CART_TOKEN_HEADER, required = false) String cartToken,
            @AuthenticationPrincipal Jwt jwt) {
        cartService.deleteItem(cartId, itemId, cartToken, jwt);
        return ResponseEntity.noContent().build();
    }
}
