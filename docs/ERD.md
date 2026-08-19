# Entity Relationship Diagram

The database is managed by Flyway. The authoritative executable schema is
[`V1__create_core_schema.sql`](../backend/src/main/resources/db/migration/V1__create_core_schema.sql).

```mermaid
erDiagram
    USERS ||--o{ CARTS : owns
    USERS ||--o{ BOOKINGS : places
    RESORTS ||--o{ PRODUCTS : offers
    PRODUCTS ||--o{ LESSON_SESSIONS : schedules
    CARTS ||--o{ CART_ITEMS : contains
    PRODUCTS ||--o{ CART_ITEMS : configures
    LESSON_SESSIONS o|--o{ CART_ITEMS : selected_by
    BOOKINGS ||--|{ BOOKING_ITEMS : snapshots
    PRODUCTS ||--o{ BOOKING_ITEMS : references
    LESSON_SESSIONS o|--o{ BOOKING_ITEMS : references
    BOOKINGS ||--o{ PAYMENTS : attempts

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar role
        timestamptz created_at
        timestamptz updated_at
    }

    RESORTS {
        bigint id PK
        varchar name UK
        varchar location
        varchar status
    }

    PRODUCTS {
        bigint id PK
        bigint resort_id FK
        varchar name
        varchar category
        numeric price
        boolean is_active
    }

    LESSON_SESSIONS {
        bigint id PK
        bigint product_id FK
        date session_date
        time start_time
        time end_time
        int capacity
        int booked_count
        varchar status
    }

    CARTS {
        bigint id PK
        bigint user_id FK
        varchar session_token UK
        varchar status
    }

    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        bigint lesson_session_id FK
        int quantity
        numeric unit_price
    }

    BOOKINGS {
        bigint id PK
        varchar booking_number UK
        bigint user_id FK
        varchar status
        varchar currency
        numeric total_amount
    }

    BOOKING_ITEMS {
        bigint id PK
        bigint booking_id FK
        bigint product_id FK
        bigint lesson_session_id FK
        varchar product_name
        varchar category
        int quantity
        numeric unit_price
        numeric subtotal
    }

    PAYMENTS {
        bigint id PK
        bigint booking_id FK
        varchar stripe_payment_id UK
        numeric amount
        varchar currency
        varchar status
    }
```

## Controlled values

- Product category: `RESORT_ACCESS`, `LIFT_TICKET`, `LESSON`, `RENTAL`
- User role: `CUSTOMER`, `ADMIN`
- Resort status: `ACTIVE`, `INACTIVE`
- Lesson session status: `ACTIVE`, `CANCELLED`
- Cart status: `ACTIVE`, `CHECKED_OUT`, `ABANDONED`
- Booking status: `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`
- Payment status: `PENDING`, `SUCCEEDED`, `FAILED`, `REFUNDED`

Database check constraints enforce these values and protect positive quantities,
non-negative monetary amounts, valid date ranges, and lesson capacity bounds.

