# ADR 0005: Stripe PaymentIntent lifecycle

- Status: Accepted
- Date: 2026-08-20

## Context

The platform must accept payment without handling raw card data, prevent double
charges, confirm orders only after Stripe reports success, and release reserved
lesson capacity without processing duplicate webhook deliveries twice.

## Decision

- The backend creates one Stripe PaymentIntent per booking and uses a stable
  booking-based idempotency key. Repeated create requests retrieve that intent.
- The browser receives only the PaymentIntent client secret. The Stripe secret
  key exists only in backend environment configuration.
- Booking number is stored as non-sensitive Stripe metadata for reconciliation.
- The signed Stripe webhook is the asynchronous source of truth. A customer-only
  confirmation endpoint can also retrieve the PaymentIntent directly from Stripe
  and reconcile the same state machine.
- `payment_intent.succeeded` confirms the booking idempotently.
- `payment_intent.payment_failed` records a failed attempt but leaves the booking
  pending because Stripe permits another payment method on the same intent.
- `payment_intent.canceled` cancels a pending booking and releases lesson capacity
  under database row locks. Booking state prevents duplicate release.
- PaymentIntent ID, amount, and currency must match the stored payment before any
  event can change local state.

## Consequences

The application never accepts a browser-provided success flag and never stores
card numbers or CVV. Retrying a declined card does not create a second intent.
Webhook delivery can be repeated safely. Operational cleanup for abandoned
PaymentIntents should cancel them through Stripe; the resulting signed cancelled
event then performs the same capacity-release path.
