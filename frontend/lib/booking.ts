export type BookingStatus = "PENDING" | "CONFIRMED" | "CANCELLED";
export type PaymentStatus = "PENDING" | "SUCCEEDED" | "FAILED";

export type BookingItem = {
  id: number;
  productId: number;
  lessonSessionId: number | null;
  productName: string;
  category: "RESORT_ACCESS" | "LIFT_TICKET" | "LESSON" | "RENTAL";
  quantity: number;
  unitPrice: number;
  subtotal: number;
  bookingDate: string | null;
  vehicleRegistration: string | null;
  vehicleType: string | null;
  entryDate: string | null;
  exitDate: string | null;
  rentalStartDate: string | null;
  rentalEndDate: string | null;
  rentalSize: string | null;
  rentalBootSize: string | null;
};

export type Booking = {
  bookingNumber: string;
  status: BookingStatus;
  currency: string;
  totalAmount: number;
  customerFirstName: string;
  customerLastName: string;
  customerEmail: string;
  customerPhone: string | null;
  createdAt: string;
  items: BookingItem[];
};

export type CreateBookingInput = {
  cartId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
};

export type Payment = {
  bookingNumber: string;
  bookingStatus: BookingStatus;
  paymentIntentId: string;
  paymentStatus: PaymentStatus;
  amount: number;
  currency: string;
  clientSecret: string | null;
};

type ApiErrorBody = { message?: string; fieldErrors?: Array<{ field: string; message: string }> };

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function authenticatedRequest<T>(path: string, accessToken: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
      ...init?.headers,
    },
  });
  if (!response.ok) {
    let body: ApiErrorBody = {};
    try { body = await response.json() as ApiErrorBody; } catch { /* Use the status fallback. */ }
    const fieldError = body.fieldErrors?.[0];
    throw new Error(fieldError ? `${fieldError.field}: ${fieldError.message}` : body.message ?? `Booking request failed (${response.status}).`);
  }
  return response.json() as Promise<T>;
}

export function createBooking(input: CreateBookingInput, accessToken: string) {
  return authenticatedRequest<Booking>("/api/bookings", accessToken, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getBooking(bookingNumber: string, accessToken: string) {
  return authenticatedRequest<Booking>(`/api/bookings/${encodeURIComponent(bookingNumber)}`, accessToken);
}

export function createPayment(bookingNumber: string, accessToken: string) {
  return authenticatedRequest<Payment>("/api/payments/create", accessToken, {
    method: "POST",
    body: JSON.stringify({ bookingNumber }),
  });
}

export function confirmPayment(bookingNumber: string, accessToken: string) {
  return authenticatedRequest<Payment>("/api/payments/confirm", accessToken, {
    method: "POST",
    body: JSON.stringify({ bookingNumber }),
  });
}

export function pendingBookingKey(userId: number) {
  return `snow-alpine-pending-booking-${userId}`;
}
