package com.skibooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skibooking.entity.Cart;
import com.skibooking.entity.enums.CartStatus;

import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findBySessionTokenAndStatus(String sessionToken, CartStatus status);

    Optional<Cart> findByIdAndStatus(Long id, CartStatus status);

    Optional<Cart> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart where cart.id = :id and cart.status = :status")
    Optional<Cart> findByIdAndStatusForUpdate(
            @Param("id") Long id,
            @Param("status") CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart where cart.sessionToken = :token and cart.status = :status")
    Optional<Cart> findBySessionTokenAndStatusForUpdate(
            @Param("token") String token,
            @Param("status") CartStatus status);
}
