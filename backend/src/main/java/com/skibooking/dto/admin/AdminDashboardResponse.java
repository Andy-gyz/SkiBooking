package com.skibooking.dto.admin;

public record AdminDashboardResponse(
        long resortAccessReservations,
        long liftTicketReservations,
        long lessonReservations,
        long rentalReservations) {
}
