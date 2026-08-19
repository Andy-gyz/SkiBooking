package com.skibooking.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skibooking.entity.BookingItem;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.ProductCategory;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    List<BookingItem> findByBookingIdOrderByIdAsc(Long bookingId);

    @Query("""
            select coalesce(sum(item.quantity), 0)
            from BookingItem item
            where item.category = :category
              and item.booking.status in :statuses
            """)
    long countReservedQuantity(
            @Param("category") ProductCategory category,
            @Param("statuses") Collection<BookingStatus> statuses);
}

