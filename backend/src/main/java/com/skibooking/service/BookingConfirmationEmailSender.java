package com.skibooking.service;

import java.util.List;

import com.skibooking.entity.Booking;
import com.skibooking.entity.BookingItem;

public interface BookingConfirmationEmailSender {

    void sendConfirmation(Booking booking, List<BookingItem> items);
}
