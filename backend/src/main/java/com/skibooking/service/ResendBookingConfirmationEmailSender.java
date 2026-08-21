package com.skibooking.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import com.skibooking.config.EmailVerificationProperties;
import com.skibooking.entity.Booking;
import com.skibooking.entity.BookingItem;
import com.skibooking.exception.EmailDeliveryException;

@Component
public class ResendBookingConfirmationEmailSender implements BookingConfirmationEmailSender {

    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM uuuu");

    private final EmailVerificationProperties properties;
    private final RestClient restClient = RestClient.create();

    public ResendBookingConfirmationEmailSender(EmailVerificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendConfirmation(Booking booking, List<BookingItem> items) {
        if (!hasText(properties.apiKey()) || !hasText(properties.from())) {
            throw new EmailDeliveryException("Email delivery is not configured.");
        }

        String itemRows = items.stream().map(this::itemRow).reduce("", String::concat);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto;padding:32px;color:#111318">
                  <div style="font-size:13px;font-weight:700;letter-spacing:.08em">SNOW ALPINE</div>
                  <h1 style="font-size:30px;margin:32px 0 12px">Your mountain day is confirmed.</h1>
                  <p style="color:#646870;line-height:1.6">Hi %s, your payment was successful and booking <strong>%s</strong> is ready.</p>
                  <div style="margin:28px 0;border:1px solid #e4e6eb;border-radius:16px;overflow:hidden">%s</div>
                  <div style="display:flex;justify-content:space-between;gap:24px;padding:20px;border-radius:16px;background:#f1f3ff">
                    <strong>Total paid</strong><strong>%s %s</strong>
                  </div>
                  <p style="color:#646870;font-size:13px;line-height:1.6;margin-top:28px">Keep this email for arrival. You can also sign in to Snow Alpine to review your booking.</p>
                </div>
                """.formatted(
                escape(booking.getCustomerFirstName()),
                escape(booking.getBookingNumber()),
                itemRows,
                escape(booking.getCurrency()),
                booking.getTotalAmount().setScale(2));

        Map<String, Object> body = Map.of(
                "from", properties.from(),
                "to", List.of(booking.getCustomerEmail()),
                "subject", "Snow Alpine booking " + booking.getBookingNumber() + " confirmed",
                "html", html);
        try {
            restClient.post()
                    .uri(RESEND_EMAILS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException("We could not send the booking confirmation email.", exception);
        }
    }

    private String itemRow(BookingItem item) {
        LocalDate date = item.getLessonSession() == null
                ? item.getBookingDate()
                : item.getLessonSession().getSessionDate();
        String dateText = date == null ? "Date confirmed in your booking" : DATE_FORMAT.format(date);
        return """
                <div style="padding:18px 20px;border-bottom:1px solid #e4e6eb">
                  <strong>%s × %d</strong><div style="color:#646870;margin-top:6px">%s</div>
                </div>
                """.formatted(escape(item.getProductName()), item.getQuantity(), escape(dateText));
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
