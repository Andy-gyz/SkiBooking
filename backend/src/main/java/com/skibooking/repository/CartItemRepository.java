package com.skibooking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skibooking.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartIdOrderByCreatedAtAsc(Long cartId);
}

