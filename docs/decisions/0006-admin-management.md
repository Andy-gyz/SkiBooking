# ADR 0006: Admin reservation metrics and safe catalog management

- Status: Accepted
- Date: 2026-08-20

## Context

The admin dashboard needs consistent totals for four product categories while
catalog management must not corrupt historical orders or existing lesson
reservations. Frontend-only route protection is insufficient.

## Decision

- Spring Security requires role `ADMIN` for all `/api/admin/**` routes.
- Dashboard totals sum booking-item quantities, not booking rows, for statuses
  `CONFIRMED` and `COMPLETED` only.
- Category reservation lists are also restricted to those valid statuses and
  return customer, product snapshot, category selections, and payment state.
- Product deletion is implemented as `active=false`. Historical rows and foreign
  keys are preserved, and public catalog queries naturally hide the product.
- Lesson capacity cannot be reduced below `booked_count`.
- A session with reservations cannot be moved to another product/date/time or
  cancelled through the basic management API. A future operational cancellation
  workflow must explicitly handle affected customers and capacity.
- A local admin can be seeded only through explicit environment variables under
  the `local` profile. Existing customer accounts are never silently elevated.

## Consequences

Dashboard cards share a stable and auditable definition. Admin catalog changes
do not rewrite booking snapshots. Some operational changes are intentionally
rejected until customer notification, refund, and rescheduling workflows exist.
