# ADR 0001: Core persistence model

- Status: Accepted
- Date: 2026-08-20

## Context

The MVP needs one consistent relational model for four product categories while
preserving historical bookings and supporting anonymous carts, lesson capacity,
payment retries, and administrator reporting.

## Decision

- PostgreSQL 17 is the development database.
- Flyway is the only mechanism that creates or changes production schema.
- Hibernate runs with `ddl-auto=validate` and cannot mutate the schema.
- All controlled states are Java enums persisted as strings and protected by
  PostgreSQL check constraints.
- Product-specific cart and booking details remain nullable columns in the shared
  item tables for MVP simplicity.
- Booking items preserve the purchased product name, category, price, quantity,
  subtotal, and category-specific configuration.
- Bookings also preserve customer contact details so later profile edits cannot
  change the historical booking record.
- Monetary records include an uppercase three-letter currency code. The initial
  default is `AUD`.
- Audit timestamps use `TIMESTAMPTZ`; Hibernate reads and writes them as UTC
  `Instant` values.
- Lesson capacity updates will load the session through a pessimistic-write
  repository query before checking and changing `booked_count`.
- Testcontainers provides a disposable PostgreSQL database for integration tests.
  Tests never depend on or mutate the developer's Compose database.
- An idempotent Repository-based seeder provides fictional data only when the
  `local` Spring profile is active. Seed data is not recorded as a production
  Flyway migration.

## Consequences

The model stays close to the nine-table MVP specification while adding customer
history, currency, rental boot-size, database constraints, and concurrency-ready
lesson access. Detailed rental inventory and refund-ledger tables remain outside
the MVP.
