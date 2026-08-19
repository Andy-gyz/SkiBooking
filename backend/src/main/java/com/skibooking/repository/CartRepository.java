package com.skibooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skibooking.entity.Cart;
import com.skibooking.entity.enums.CartStatus;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findBySessionTokenAndStatus(String sessionToken, CartStatus status);

    Optional<Cart> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, CartStatus status);
}

