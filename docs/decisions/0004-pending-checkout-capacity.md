# ADR 0004: Pending checkout and lesson capacity reservation

- Status: Accepted
- Date: 2026-08-20

## Context

Checkout must preserve historical product and price data, reject stale carts,
and prevent concurrent lesson purchases from exceeding session capacity. Payment
is a separate milestone, so order creation cannot yet imply successful payment.

## Decision

- Checkout requires an authenticated customer who owns the active cart.
- The backend reloads available products, validates all category-specific data,
  and calculates totals from current trusted prices.
- A booking and its item snapshots are created atomically with status `PENDING`;
  the source cart becomes `CHECKED_OUT` in the same transaction.
- Booking items copy product name, category, unit price, subtotal, quantity, and
  the category-specific selections so later catalog changes do not alter history.
- Each lesson session is selected with a pessimistic database write lock. Its
  `booked_count` is incremented before the transaction commits, ensuring two
  concurrent checkouts cannot consume the same remaining places.
- Customer booking detail is looked up using both booking number and authenticated
  user ID. Inaccessible and nonexistent bookings share `BOOKING_NOT_FOUND`.

## Consequences

Creating a `PENDING` booking temporarily reserves lesson capacity before payment
has succeeded. The payment workflow must confirm the booking idempotently and
release reserved capacity exactly once when payment fails, expires, or the order
is cancelled. Until that workflow exists, pending reservations do not expire
automatically. Booking-number uniqueness is enforced by the database, while the
application generates a date-prefixed random public identifier.
