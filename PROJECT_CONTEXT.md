
# Ski Booking Platform — PROJECT_CONTEXT

## 1. Project Overview

This project is a production-style full-stack ski booking platform intended to be deployed publicly with a formally purchased domain name.

The platform has two main user groups:

1. Customers
2. Administrators

The core booking experience is based around four main product/service categories:

1. Resort Entry & Parking
2. Lift Tickets
3. Lessons
4. Rentals

Customers can browse these categories from the home page, view available products, add products to a shopping cart without logging in, and only be required to log in or register when proceeding to checkout.

Administrators have a dedicated admin dashboard. The admin home page must show the booking/reservation count for the same four categories. The administrator can click any category to view detailed reservation information.

The project must include:

- A real purchased domain
- Frontend
- Backend
- Database
- Authentication
- Customer-facing website
- Admin dashboard
- Shopping cart
- Checkout
- Payment
- Booking records
- Reservation detail management
- Deployment
- HTTPS
- Version control

The project should be treated as a real commercial-style MVP rather than a simple university CRUD demo.

---

# 2. Core Product Definition

## 2.1 Customer-facing modules

The customer home page must provide direct access to:

- Resort Entry
- Lift Tickets
- Lessons
- Rentals

These four customer-facing modules map to the following backend product categories:

```text
RESORT_ACCESS
LIFT_TICKET
LESSON
RENTAL
```

Important terminology:

## Resort

A `resort` in the database represents an actual ski resort/location, for example:

- Mt Buller
- Falls Creek
- Mt Hotham

## Resort Entry

The customer-facing `Resort Entry` section does NOT mean the ski resort entity itself.

It means vehicle access / mountain access / resort parking products required when driving into the resort area.

Examples:

- Daily Vehicle Entry
- Overnight Parking
- Multi-Day Parking
- Weekend Parking Pass

Internally, this category should be called:

```text
RESORT_ACCESS
```

This avoids confusion with the `resorts` database table.

---

# 3. Primary Customer Journey

The required customer flow is:

```text
Home
  ↓
Choose one of:
  ├── Resort Entry
  ├── Lift Tickets
  ├── Lessons
  └── Rentals
  ↓
Browse products
  ↓
Product Detail
  ↓
Configure booking details
  ↓
Add to Cart
  ↓
Continue browsing / add more products
  ↓
Cart
  ↓
Proceed to Checkout
  ↓
If not logged in:
  ├── Login
  └── Register
  ↓
Customer details
  ↓
Payment
  ↓
Booking Confirmation
  ↓
My Bookings
```

Important requirement:

Customers must NOT be forced to log in before browsing or adding products to the cart.

Anonymous shopping is allowed.

Authentication is required only when proceeding to checkout.

After login or registration, the existing anonymous cart must remain intact and become associated with the authenticated user.

---

# 4. Customer Home Page

The home page should immediately show the four primary booking modules.

Suggested structure:

```text
------------------------------------------------
Logo / Navigation
------------------------------------------------

Plan Your Snow Adventure

┌────────────────────┐
│ Resort Entry       │
│ Entry & Parking    │
│ View & Book        │
└────────────────────┘

┌────────────────────┐
│ Lift Tickets       │
│ View & Book        │
└────────────────────┘

┌────────────────────┐
│ Lessons            │
│ View & Book        │
└────────────────────┘

┌────────────────────┐
│ Rentals            │
│ View & Book        │
└────────────────────┘
```

Suggested header:

```text
Logo
Home
Resort Entry
Lift Tickets
Lessons
Rentals
Cart
Login / Account
```

---

# 5. Customer Module Details

## 5.1 Resort Entry & Parking

This module is specifically for customers driving into the mountain/resort.

Possible products:

- Daily Vehicle Entry
- Overnight Parking
- 2-Day Parking Pass
- 3-Day Parking Pass
- Weekend Parking Pass

Example product:

```text
Overnight Parking

Entry Date:
25 Aug 2026

Exit Date:
26 Aug 2026

Vehicle Registration:
ABC123

Vehicle Type:
SUV

Quantity:
1

[ Add to Cart ]
```

Important Resort Access data:

- Entry date
- Exit date
- Vehicle registration / licence plate
- Vehicle type
- Product type
- Quantity
- Price

The system should store the vehicle registration because resort access and parking reservations are vehicle-related.

Backend category:

```text
RESORT_ACCESS
```

---

## 5.2 Lift Tickets

Possible products:

- Adult Full Day Lift Pass
- Adult Half Day Lift Pass
- Child Full Day Lift Pass
- Child Half Day Lift Pass
- Multi-Day Lift Pass

Example:

```text
Adult Full Day Lift Pass

Date:
25 Aug 2026

Quantity:
2

Price:
$135 each

[ Add to Cart ]
```

Important Lift Ticket data:

- Product
- Booking date
- Quantity
- Unit price
- Resort

Backend category:

```text
LIFT_TICKET
```

---

## 5.3 Lessons

Possible products:

- Beginner Ski Lesson
- Intermediate Ski Lesson
- Private Lesson
- Group Lesson
- Snowboard Lesson

Lessons are different from ordinary products because they have session capacity and time slots.

Example:

```text
Beginner Ski Lesson

Date:
25 Aug 2026

Time:
10:00 AM – 12:00 PM

Participants:
1

Level:
Beginner

Price:
$120

Available:
2 spots

[ Add to Cart ]
```

Lesson session example:

```text
10:00 AM Beginner Lesson
Capacity: 8
Booked: 6
Available: 2
```

If:

```text
Available = 0
```

the customer-facing interface must show:

```text
SOLD OUT
```

and prevent booking.

Backend category:

```text
LESSON
```

---

## 5.4 Rentals

Possible products:

- Ski Package
- Snowboard Package
- Ski Boots
- Snowboard Boots
- Helmet
- Jacket
- Pants

Example:

```text
Ski Package

Rental Start:
25 Aug 2026

Rental End:
26 Aug 2026

Size:
Adult Medium / 170cm

Boot Size:
US 9

Quantity:
1

[ Add to Cart ]
```

For MVP, do NOT build a complex equipment-level inventory system with serial numbers.

Rental-specific information can initially be stored directly on cart items and booking items.

Backend category:

```text
RENTAL
```

---

# 6. Shopping Cart

All four product categories use one unified shopping cart.

Example cart:

```text
Your Cart

2 × Adult Resort Entry
$100

2 × Adult Full Day Lift Ticket
$270

1 × Beginner Lesson
$120

1 × Ski Rental
$65

------------------------
Subtotal: $555
Total:    $555

[ Proceed to Checkout ]
```

Core rules:

- Customers can add products from any of the four categories.
- Products from multiple categories can be checked out together.
- The customer should not have to complete separate transactions for each category.
- Anonymous cart must persist until login/register.
- Cart must not disappear after authentication.

---

# 7. Authentication Flow

Required authentication functions:

- Register
- Login
- Logout
- Get current authenticated user
- Role-based authorization

User roles:

```text
CUSTOMER
ADMIN
```

Customer checkout logic:

```text
Cart
  ↓
Proceed to Checkout
  ↓
Is authenticated?
  ├── Yes → Checkout
  └── No
       ↓
    Login / Register
       ↓
    Preserve cart
       ↓
    Checkout
```

Important security rule:

Admin access must NOT be enforced only by hiding admin pages in the frontend.

The Spring Boot backend must also reject non-admin users from `/api/admin/**`.

---

# 8. Checkout

Suggested checkout sections:

## Customer Details

- First name
- Last name
- Email
- Phone

## Booking Summary

Show all products from the cart.

## Payment

Use Stripe.

The application must NOT store raw bank card information.

Stripe handles sensitive payment information.

Example:

```text
Payment

Card information handled by Stripe

Total:
$535

[ Pay $535 ]
```

---

# 9. Booking Confirmation

Example:

```text
Booking Confirmed

Booking Number:
SKI-20260825-0001

Resort:
Mt Buller

Products:
2 × Adult Lift Ticket
1 × Beginner Lesson
1 × Ski Rental

Total Paid:
$535

Status:
CONFIRMED

[ View My Booking ]
```

Optional later enhancement:

- Email booking confirmation

---

# 10. Customer Account

Suggested structure:

```text
My Account
├── Profile
└── My Bookings
    ├── Upcoming
    ├── Past
    └── Cancelled
```

Booking card example:

```text
SKI-20260825-0001

Mt Buller
25 Aug 2026
$535
CONFIRMED

[ View ]
```

---

# 11. Admin Requirements

The admin experience must correspond directly to the four customer-facing categories.

Customer:

```text
Resort Entry
Lift Tickets
Lessons
Rentals
```

Admin:

```text
Resort Entry Reservations
Lift Ticket Reservations
Lesson Reservations
Rental Reservations
```

---

# 12. Admin Dashboard

Admin home page must show reservation counts for all four categories at a glance.

Example:

```text
ADMIN DASHBOARD

Resort Entry
126 Reservations

Lift Tickets
94 Reservations

Lessons
37 Reservations

Rentals
68 Reservations
```

Each card must be clickable.

Clicking a card opens a detailed reservation list for that category.

Optional enhanced dashboard metrics:

- Today's reservations
- Today's revenue
- Total revenue
- Upcoming bookings
- Customer count

These are useful but are not mandatory for the first MVP.

---

# 13. Admin Reservation Detail Views

## 13.1 Resort Entry Reservations

List view:

```text
Date       Customer      Product              Vehicle    Status

25 Aug     Andy Zhu      Daily Entry          ABC123     Confirmed
25 Aug     John Smith    Overnight Parking    XYZ789     Confirmed
26 Aug     Amy Lee       Daily Entry          AAA888     Confirmed
```

Detail view:

```text
Booking ID:
SKI-00125

Customer:
Andy Zhu

Product:
Overnight Parking

Entry:
25 Aug 2026

Exit:
26 Aug 2026

Vehicle Registration:
ABC123

Vehicle Type:
SUV

Payment:
Paid

Booking Status:
Confirmed
```

---

## 13.2 Lift Ticket Reservations

Suggested fields:

- Booking ID
- Customer
- Ticket type
- Date
- Quantity
- Payment status
- Booking status

---

## 13.3 Lesson Reservations

Lessons should support both reservation listing and session-level views.

Example:

```text
25 Aug 2026

10:00 AM
Beginner Lesson
Booked: 7 / 8

1:00 PM
Beginner Lesson
Booked: 5 / 8
```

Clicking a session can show participants.

Suggested fields:

- Customer
- Session
- Date
- Start time
- End time
- Quantity / participants
- Booking status

---

## 13.4 Rental Reservations

Suggested fields:

- Customer
- Product
- Pickup/start date
- Return/end date
- Rental size
- Quantity
- Booking status

---

# 14. Customer Sitemap V1

```text
Home
│
├── Resort Entry
│   └── Product Detail
│
├── Lift Tickets
│   └── Product Detail
│
├── Lessons
│   └── Lesson Detail
│
├── Rentals
│   └── Rental Detail
│
├── Cart
│
├── Login
│
├── Register
│
├── Checkout
│
├── Payment
│
├── Booking Confirmation
│
└── My Account
    ├── Profile
    └── My Bookings
```

---

# 15. Admin Sitemap V1

```text
Admin Login
│
└── Dashboard
    │
    ├── Resort Entry Reservations
    │   └── Reservation Detail
    │
    ├── Lift Ticket Reservations
    │   └── Reservation Detail
    │
    ├── Lesson Reservations
    │   └── Reservation Detail
    │
    ├── Rental Reservations
    │   └── Reservation Detail
    │
    ├── Customers
    │
    ├── Products
    │
    └── Settings
```

---

# 16. Recommended Technology Stack

## Frontend

```text
Next.js
React
TypeScript
Tailwind CSS
```

Use Next.js App Router.

## Backend

```text
Spring Boot
Java
Spring Security
REST API
```

## Database

```text
PostgreSQL
```

## Authentication

Recommended direction:

```text
JWT-based authentication
Spring Security
Role-based authorization
```

Roles:

```text
CUSTOMER
ADMIN
```

## Payment

```text
Stripe
```

## Image Storage

```text
AWS S3
```

## Deployment

Recommended:

```text
Frontend:
Vercel

Backend:
AWS

Database:
AWS RDS PostgreSQL

Images:
AWS S3
```

## Source Control

```text
Git
GitHub
```

---

# 17. Domain Requirement

The website must use a formally purchased real domain.

Final structure can eventually be:

```text
www.yourdomain.com
```

Customer site.

Possible admin setup:

```text
www.yourdomain.com/admin
```

For MVP, this is simpler.

A future alternative is:

```text
admin.yourdomain.com
```

API can eventually be:

```text
api.yourdomain.com
```

Potential registrars:

- Cloudflare Registrar
- Namecheap
- GoDaddy
- VentraIP

Do not purchase a domain until the final brand/project name is decided.

---

# 18. Database Design V1

The MVP currently uses nine main tables:

```text
users
resorts
products
lesson_sessions
carts
cart_items
bookings
booking_items
payments
```

Do NOT add unnecessary complex tables in V1 such as:

- coupons
- loyalty_points
- equipment_serial_numbers
- refund_history
- instructor_accounts
- partner_accounts
- chat_messages
- AI_recommendations

Those can be introduced later.

---

# 19. Table: users

Purpose:

Stores both customer and administrator accounts.

```text
users
-------------------------------------------
id              BIGSERIAL PRIMARY KEY
first_name      VARCHAR(100) NOT NULL
last_name       VARCHAR(100) NOT NULL
email           VARCHAR(255) UNIQUE NOT NULL
password_hash   VARCHAR(255) NOT NULL
phone           VARCHAR(30) NULL
role            VARCHAR(20) NOT NULL
created_at      TIMESTAMP NOT NULL
updated_at      TIMESTAMP NOT NULL
```

Role values:

```text
CUSTOMER
ADMIN
```

Important:

Passwords must never be stored in plain text.

Use secure hashing via Spring Security / BCrypt.

---

# 20. Table: resorts

Purpose:

Represents actual ski resorts / ski locations.

Examples:

- Mt Buller
- Falls Creek
- Mt Hotham

```text
resorts
-------------------------------------------
id              BIGSERIAL PRIMARY KEY
name            VARCHAR(150) NOT NULL
location        VARCHAR(255) NOT NULL
description     TEXT NULL
image_url       VARCHAR(500) NULL
status          VARCHAR(20) NOT NULL
created_at      TIMESTAMP NOT NULL
updated_at      TIMESTAMP NOT NULL
```

Suggested status values:

```text
ACTIVE
INACTIVE
```

Important:

This is NOT the same as the customer-facing `Resort Entry` product category.

---

# 21. Table: products

Purpose:

Stores products for all four booking categories.

```text
products
-------------------------------------------
id              BIGSERIAL PRIMARY KEY
resort_id       BIGINT NOT NULL
name            VARCHAR(150) NOT NULL
category        VARCHAR(30) NOT NULL
description     TEXT NULL
price           DECIMAL(10,2) NOT NULL
image_url       VARCHAR(500) NULL
is_active       BOOLEAN NOT NULL
created_at      TIMESTAMP NOT NULL
updated_at      TIMESTAMP NOT NULL

FOREIGN KEY (resort_id) REFERENCES resorts(id)
```

Allowed categories:

```text
RESORT_ACCESS
LIFT_TICKET
LESSON
RENTAL
```

Example rows:

```text
Daily Vehicle Entry
RESORT_ACCESS

Overnight Parking
RESORT_ACCESS

Adult Full Day Lift Pass
LIFT_TICKET

Beginner Ski Lesson
LESSON

Ski Package
RENTAL
```

Rationale:

The four categories share common attributes:

- Name
- Description
- Price
- Image
- Resort
- Active status

Using a single products table keeps Cart and Booking logic significantly simpler.

---

# 22. Table: lesson_sessions

Purpose:

Stores scheduled instances of lesson products.

A lesson product may have many sessions.

```text
lesson_sessions
-------------------------------------------
id              BIGSERIAL PRIMARY KEY
product_id      BIGINT NOT NULL
session_date    DATE NOT NULL
start_time      TIME NOT NULL
end_time        TIME NOT NULL
capacity        INT NOT NULL
booked_count    INT NOT NULL DEFAULT 0
status          VARCHAR(20) NOT NULL

FOREIGN KEY (product_id) REFERENCES products(id)
```

Example:

```text
Product:
Beginner Ski Lesson

Date:
25 Aug 2026

Time:
10:00 - 12:00

Capacity:
8

Booked:
6
```

Relationship:

```text
Product 1 : N LessonSession
```

Business rule:

```text
available = capacity - booked_count
```

If available <= 0:

- show SOLD OUT
- reject new booking attempts

Backend validation must enforce capacity.

Do not rely only on frontend state.

---

# 23. Table: carts

Purpose:

Stores shopping carts, including anonymous carts.

```text
carts
-------------------------------------------
id              BIGSERIAL PRIMARY KEY
user_id         BIGINT NULL
session_token   VARCHAR(255) NULL
status          VARCHAR(20) NOT NULL
created_at      TIMESTAMP NOT NULL
updated_at      TIMESTAMP NOT NULL

FOREIGN KEY (user_id) REFERENCES users(id)
```

Suggested statuses:

```text
ACTIVE
CHECKED_OUT
ABANDONED
```

Anonymous state:

```text
user_id = NULL
session_token = generated token
```

After customer login/register:

```text
user_id = authenticated user id
```

while retaining existing cart contents.

---

# 24. Table: cart_items

Purpose:

Stores items added to a cart.

```text
cart_items
---------------------------------------------------
id                      BIGSERIAL PRIMARY KEY
cart_id                 BIGINT NOT NULL
product_id              BIGINT NOT NULL
lesson_session_id       BIGINT NULL

quantity                INT NOT NULL
unit_price              DECIMAL(10,2) NOT NULL

booking_date            DATE NULL

vehicle_registration    VARCHAR(30) NULL
vehicle_type            VARCHAR(50) NULL
entry_date              DATE NULL
exit_date               DATE NULL

rental_start_date       DATE NULL
rental_end_date         DATE NULL
rental_size             VARCHAR(50) NULL

created_at              TIMESTAMP NOT NULL

FOREIGN KEY (cart_id) REFERENCES carts(id)
FOREIGN KEY (product_id) REFERENCES products(id)
FOREIGN KEY (lesson_session_id) REFERENCES lesson_sessions(id)
```

How fields are used:

### RESORT_ACCESS

Uses:

```text
vehicle_registration
vehicle_type
entry_date
exit_date
```

### LIFT_TICKET

Uses:

```text
booking_date
```

### LESSON

Uses:

```text
lesson_session_id
```

### RENTAL

Uses:

```text
rental_start_date
rental_end_date
rental_size
```

Unused fields remain `NULL`.

This intentionally keeps V1 simple.

---

# 25. Table: bookings

Purpose:

Represents a completed/created booking order.

```text
bookings
-------------------------------------------
id              BIGSERIAL PRIMARY KEY
booking_number  VARCHAR(50) UNIQUE NOT NULL
user_id         BIGINT NOT NULL
status          VARCHAR(20) NOT NULL
total_amount    DECIMAL(10,2) NOT NULL
created_at      TIMESTAMP NOT NULL
updated_at      TIMESTAMP NOT NULL

FOREIGN KEY (user_id) REFERENCES users(id)
```

Suggested booking status values:

```text
PENDING
CONFIRMED
CANCELLED
COMPLETED
```

Example booking number:

```text
SKI-20260825-0001
```

---

# 26. Table: booking_items

Purpose:

Stores the finalized contents of a booking.

This table is a historical snapshot of the cart at checkout.

```text
booking_items
---------------------------------------------------
id                      BIGSERIAL PRIMARY KEY
booking_id              BIGINT NOT NULL
product_id              BIGINT NOT NULL
lesson_session_id       BIGINT NULL

product_name            VARCHAR(150) NOT NULL
category                VARCHAR(30) NOT NULL

quantity                INT NOT NULL
unit_price              DECIMAL(10,2) NOT NULL
subtotal                DECIMAL(10,2) NOT NULL

booking_date            DATE NULL

vehicle_registration    VARCHAR(30) NULL
vehicle_type            VARCHAR(50) NULL
entry_date              DATE NULL
exit_date               DATE NULL

rental_start_date       DATE NULL
rental_end_date         DATE NULL
rental_size             VARCHAR(50) NULL

FOREIGN KEY (booking_id) REFERENCES bookings(id)
FOREIGN KEY (product_id) REFERENCES products(id)
FOREIGN KEY (lesson_session_id) REFERENCES lesson_sessions(id)
```

Important rationale:

Even though products already store name and price, booking_items must store:

```text
product_name
category
unit_price
```

because bookings must preserve historical purchase data.

Example:

Customer buys ticket at:

```text
$120
```

Admin later changes current product price to:

```text
$150
```

The old booking must still display:

```text
$120
```

Therefore booking_items act as an immutable purchase snapshot.

---

# 27. Table: payments

Purpose:

Stores payment attempts and results.

```text
payments
-------------------------------------------
id                  BIGSERIAL PRIMARY KEY
booking_id          BIGINT NOT NULL
stripe_payment_id   VARCHAR(255) NULL
amount              DECIMAL(10,2) NOT NULL
status              VARCHAR(20) NOT NULL
payment_method      VARCHAR(50) NULL
paid_at             TIMESTAMP NULL
created_at          TIMESTAMP NOT NULL

FOREIGN KEY (booking_id) REFERENCES bookings(id)
```

Suggested statuses:

```text
PENDING
SUCCEEDED
FAILED
REFUNDED
```

Relationship:

```text
Booking 1 : N Payments
```

This is preferred over strict 1:1 because it allows:

- Failed payment attempts
- Payment retries
- Future refund records/extensions

---

# 28. ERD Relationships

High-level relationship model:

```text
RESORTS
   1
   │
   N
PRODUCTS
   │
   ├────────────< CART_ITEMS
   │
   ├────────────< BOOKING_ITEMS
   │
   └──── 1 : N ──── LESSON_SESSIONS


USERS
   │
   ├──── 1 : N ──── CARTS
   │                  │
   │                  └──── 1 : N ──── CART_ITEMS
   │
   └──── 1 : N ──── BOOKINGS
                      │
                      ├──── 1 : N ──── BOOKING_ITEMS
                      │
                      └──── 1 : N ──── PAYMENTS
```

Simplified:

```text
USERS
  │
  ├───────────────< CARTS
  │                    │
  │                    └────< CART_ITEMS >──── PRODUCTS
  │                                           │
  │                                           └────< LESSON_SESSIONS
  │
  └───────────────< BOOKINGS
                       │
                       ├────< BOOKING_ITEMS >──── PRODUCTS
                       │
                       └────< PAYMENTS

RESORTS
   └────< PRODUCTS
```

---

# 29. Admin Dashboard Data Logic

Admin dashboard must display reservation totals based on booking items.

Example categories:

```text
RESORT_ACCESS
LIFT_TICKET
LESSON
RENTAL
```

Dashboard response example:

```json
{
  "resortAccessReservations": 126,
  "liftTicketReservations": 94,
  "lessonReservations": 37,
  "rentalReservations": 68
}
```

Counts should normally be based on valid booking statuses only.

Recommended MVP logic:

Count:

```text
CONFIRMED
```

Possibly also:

```text
COMPLETED
```

Do NOT count cancelled bookings in active reservation totals.

The exact definition should be implemented consistently in `AdminDashboardService`.

---

# 30. REST API V1

The API should remain relatively small and focused for the first version.

Target approximately 20 core endpoints.

---

# 31. Authentication APIs

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

Responsibilities:

## Register

- Validate email
- Ensure unique email
- Hash password
- Create `CUSTOMER` user
- Return auth result

## Login

- Validate credentials
- Return JWT/session authentication
- Support cart association after login

## Logout

- End authentication as appropriate for chosen JWT strategy

## Me

- Return current authenticated user

---

# 32. Public Customer Product APIs

```text
GET /api/resorts
GET /api/resorts/{id}

GET /api/products
GET /api/products/{id}

GET /api/products?category=RESORT_ACCESS
GET /api/products?category=LIFT_TICKET
GET /api/products?category=LESSON
GET /api/products?category=RENTAL

GET /api/lesson-sessions?productId={id}&date={date}
```

Customer pages map directly to category filters.

---

# 33. Cart APIs

```text
POST   /api/carts
GET    /api/carts/{cartId}

POST   /api/carts/{cartId}/items
PUT    /api/carts/{cartId}/items/{itemId}
DELETE /api/carts/{cartId}/items/{itemId}
```

Example Lift Ticket request:

```json
{
  "productId": 5,
  "quantity": 2,
  "bookingDate": "2026-08-25"
}
```

Example Resort Access request:

```json
{
  "productId": 1,
  "quantity": 1,
  "vehicleRegistration": "ABC123",
  "vehicleType": "SUV",
  "entryDate": "2026-08-25",
  "exitDate": "2026-08-26"
}
```

Example Lesson request:

```json
{
  "productId": 8,
  "lessonSessionId": 21,
  "quantity": 1
}
```

Example Rental request:

```json
{
  "productId": 12,
  "quantity": 1,
  "rentalStartDate": "2026-08-25",
  "rentalEndDate": "2026-08-26",
  "rentalSize": "Adult Medium"
}
```

---

# 34. Booking APIs

```text
POST /api/bookings
GET  /api/bookings/{bookingNumber}
GET  /api/my-bookings
```

`POST /api/bookings` should:

1. Require authentication
2. Validate cart ownership
3. Validate cart is `ACTIVE`
4. Revalidate product availability
5. Revalidate lesson capacity
6. Calculate trusted totals on backend
7. Create Booking
8. Copy cart items into BookingItems
9. Preserve price snapshots
10. Continue to payment flow

Do NOT trust total price calculated only by frontend.

Backend must calculate totals from trusted data.

---

# 35. Payment APIs

Initial conceptual endpoints:

```text
POST /api/payments/create
POST /api/payments/confirm
```

Stripe implementation details may later change based on the chosen Stripe integration pattern.

Payment rules:

- Never store raw card data
- Store Stripe payment reference
- Booking becomes `CONFIRMED` only after successful payment
- Failed payments must not create a confirmed booking
- Handle duplicate confirmation safely
- Payment webhook support should eventually be considered

---

# 36. Admin APIs

## Dashboard

```text
GET /api/admin/dashboard
```

Example:

```json
{
  "resortAccessReservations": 126,
  "liftTicketReservations": 94,
  "lessonReservations": 37,
  "rentalReservations": 68
}
```

## Reservation Lists

```text
GET /api/admin/bookings?category=RESORT_ACCESS
GET /api/admin/bookings?category=LIFT_TICKET
GET /api/admin/bookings?category=LESSON
GET /api/admin/bookings?category=RENTAL
```

Support filters later such as:

```text
date
status
customer
bookingNumber
```

## Reservation Detail

```text
GET /api/admin/bookings/{id}
```

## Product Management

```text
POST   /api/admin/products
PUT    /api/admin/products/{id}
DELETE /api/admin/products/{id}
```

For production-style behavior, prefer soft disabling via:

```text
is_active = false
```

rather than physically deleting products that are referenced by historical bookings.

## Lesson Session Management

```text
POST /api/admin/lesson-sessions
PUT  /api/admin/lesson-sessions/{id}
```

---

# 37. Spring Boot Project Structure V1

Recommended package structure:

```text
backend/
└── src/main/java/com/skibooking/
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    ├── dto/
    ├── security/
    ├── config/
    └── exception/
```

---

# 38. Spring Boot Controllers

## AuthController

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

## ProductController

```text
GET /api/resorts
GET /api/resorts/{id}

GET /api/products
GET /api/products/{id}

GET /api/lesson-sessions
```

## CartController

```text
POST   /api/carts
GET    /api/carts/{cartId}
POST   /api/carts/{cartId}/items
PUT    /api/carts/{cartId}/items/{itemId}
DELETE /api/carts/{cartId}/items/{itemId}
```

## BookingController

```text
POST /api/bookings
GET  /api/bookings/{bookingNumber}
GET  /api/my-bookings
```

## PaymentController

```text
POST /api/payments/create
POST /api/payments/confirm
```

## AdminDashboardController

```text
GET /api/admin/dashboard
```

## AdminBookingController

```text
GET /api/admin/bookings
GET /api/admin/bookings/{id}
```

## AdminProductController

```text
POST   /api/admin/products
PUT    /api/admin/products/{id}
DELETE /api/admin/products/{id}
```

## AdminLessonController

```text
POST /api/admin/lesson-sessions
PUT  /api/admin/lesson-sessions/{id}
```

---

# 39. Spring Boot Services

Suggested service layer:

```text
AuthService
ProductService
CartService
BookingService
PaymentService
AdminDashboardService
AdminBookingService
AdminProductService
LessonSessionService
```

Important architectural rule:

Controllers should remain thin.

Business logic belongs in services.

Repositories should primarily handle database access.

Do not place large business workflows directly inside controllers.

---

# 40. Spring Boot Repositories

Suggested repositories:

```text
UserRepository
ResortRepository
ProductRepository
LessonSessionRepository
CartRepository
CartItemRepository
BookingRepository
BookingItemRepository
PaymentRepository
```

Use Spring Data JPA unless another persistence strategy is intentionally selected later.

---

# 41. Spring Boot Entities

Entity classes map to the nine core database tables:

```text
User
Resort
Product
LessonSession
Cart
CartItem
Booking
BookingItem
Payment
```

Prefer enums for controlled values such as:

```text
UserRole
ProductCategory
BookingStatus
PaymentStatus
CartStatus
ResortStatus
LessonSessionStatus
```

---

# 42. DTO Strategy

Do not expose JPA entities directly as public API contracts.

Create DTOs.

Example groups:

```text
dto/
├── auth/
├── product/
├── cart/
├── booking/
├── payment/
└── admin/
```

Examples:

```text
RegisterRequest
LoginRequest
AuthResponse

ProductResponse
LessonSessionResponse

CreateCartItemRequest
UpdateCartItemRequest
CartResponse

CreateBookingRequest
BookingResponse
BookingSummaryResponse

AdminDashboardResponse
AdminBookingListResponse
AdminBookingDetailResponse
```

---

# 43. Next.js Frontend Structure V1

Use App Router.

```text
frontend/
└── app/
    ├── page.tsx
    │
    ├── resort-entry/
    │   ├── page.tsx
    │   └── [id]/
    │       └── page.tsx
    │
    ├── lift-tickets/
    │   ├── page.tsx
    │   └── [id]/
    │       └── page.tsx
    │
    ├── lessons/
    │   ├── page.tsx
    │   └── [id]/
    │       └── page.tsx
    │
    ├── rentals/
    │   ├── page.tsx
    │   └── [id]/
    │       └── page.tsx
    │
    ├── cart/
    │   └── page.tsx
    │
    ├── login/
    │   └── page.tsx
    │
    ├── register/
    │   └── page.tsx
    │
    ├── checkout/
    │   └── page.tsx
    │
    ├── booking-confirmation/
    │   └── [bookingNumber]/
    │       └── page.tsx
    │
    └── account/
        ├── page.tsx
        └── bookings/
            └── page.tsx
```

---

# 44. Admin Frontend Routes

Keep admin in the same Next.js project for MVP.

```text
app/
└── admin/
    ├── login/
    │   └── page.tsx
    ├── page.tsx
    ├── resort-entry/
    │   └── page.tsx
    ├── lift-tickets/
    │   └── page.tsx
    ├── lessons/
    │   └── page.tsx
    ├── rentals/
    │   └── page.tsx
    ├── bookings/
    │   └── [id]/
    │       └── page.tsx
    └── products/
        └── page.tsx
```

Mappings:

```text
/admin
→ GET /api/admin/dashboard

/admin/resort-entry
→ GET /api/admin/bookings?category=RESORT_ACCESS

/admin/lift-tickets
→ GET /api/admin/bookings?category=LIFT_TICKET

/admin/lessons
→ GET /api/admin/bookings?category=LESSON

/admin/rentals
→ GET /api/admin/bookings?category=RENTAL
```

---

# 45. Frontend Components

Suggested component organization:

```text
components/
├── customer/
│   ├── Header.tsx
│   ├── Footer.tsx
│   ├── ProductCard.tsx
│   ├── ProductGrid.tsx
│   ├── CartItem.tsx
│   └── BookingSummary.tsx
│
├── admin/
│   ├── AdminSidebar.tsx
│   ├── DashboardCard.tsx
│   ├── ReservationTable.tsx
│   └── AdminHeader.tsx
│
└── common/
    ├── Button.tsx
    ├── Input.tsx
    ├── Modal.tsx
    └── Loading.tsx
```

Do not duplicate the same UI structures across pages if they can become reusable components.

---

# 46. Frontend API Service Layer

Keep API logic outside page components.

Suggested:

```text
services/
├── authService.ts
├── productService.ts
├── cartService.ts
├── bookingService.ts
├── paymentService.ts
└── adminService.ts
```

Avoid scattering raw `fetch()` calls throughout many page files.

Use a centralized API client/helper if useful.

---

# 47. Frontend Types

Suggested:

```text
types/
├── user.ts
├── product.ts
├── cart.ts
├── booking.ts
├── payment.ts
└── admin.ts
```

Keep TypeScript types aligned with backend DTOs.

---

# 48. Suggested Repository Structure

Use a single repository containing both frontend and backend.

```text
ski-booking-platform/
│
├── frontend/
│   ├── app/
│   ├── components/
│   ├── services/
│   ├── types/
│   └── ...
│
├── backend/
│   ├── src/
│   └── ...
│
├── docs/
│   ├── ERD.md
│   ├── API.md
│   ├── architecture.md
│   └── decisions.md
│
├── PROJECT_CONTEXT.md
└── README.md
```

Codex should treat this `PROJECT_CONTEXT.md` as the authoritative current product and architecture specification unless the user explicitly changes a requirement.

---

# 49. High-Level System Architecture

Target architecture:

```text
Customer Browser
        │
        ▼
   Next.js Frontend
        │
        │ HTTPS / REST
        ▼
   Spring Boot Backend
        │
        ├──────────────► PostgreSQL
        │
        ├──────────────► Stripe
        │
        └──────────────► AWS S3
```

Admin browser uses the same Next.js frontend project:

```text
/admin/*
```

and calls protected:

```text
/api/admin/*
```

endpoints.

Deployment concept:

```text
Purchased Domain
      │
      ▼
    Vercel
  Next.js Frontend
      │
      ▼
     AWS
Spring Boot Backend
      │
      ├── AWS RDS PostgreSQL
      └── AWS S3
```

---

# 50. Security Rules

These are implementation requirements.

## Authentication

- Passwords must be hashed.
- Do not log raw passwords.
- Do not expose `password_hash` through APIs.
- Protect customer endpoints that require identity.
- Protect all admin endpoints with `ADMIN` authorization.

## Authorization

A customer must not be able to access:

```text
/api/admin/**
```

even by manually calling the API.

## Pricing

Do not trust the frontend total.

Backend must calculate:

```text
subtotal
total
```

from trusted product/pricing data.

## Cart Ownership

Once a cart is attached to a user, another user must not be able to access or mutate it by guessing the cart ID.

## Booking Ownership

Customer:

```text
GET /api/bookings/{bookingNumber}
```

must verify that the booking belongs to the authenticated customer, unless accessed through an admin endpoint.

## Stripe

Never store:

- Full card number
- CVV
- Raw sensitive card data

## Admin

Frontend route protection is not enough.

Backend authorization is mandatory.

---

# 51. Important Business Rules

## Anonymous Cart

Customers can:

```text
Browse
Add to Cart
Modify Cart
```

without logging in.

Checkout requires authentication.

On login/register:

- Preserve anonymous cart
- Associate it with authenticated user
- Avoid duplicate carts if possible

## Product Categories

Backend category names are fixed as:

```text
RESORT_ACCESS
LIFT_TICKET
LESSON
RENTAL
```

## Historical Booking Data

BookingItem must preserve:

- Purchased product name
- Purchased category
- Purchased unit price
- Quantity
- Subtotal
- Product-specific booking details

Admin changes to current Product must not rewrite historical booking data.

## Lesson Capacity

Before a lesson booking is confirmed:

- Verify session exists
- Verify session active
- Verify available capacity
- Reject if insufficient

Prevent overbooking at backend/database transaction level.

## Resort Access

Resort Access must support:

- Vehicle registration
- Vehicle type
- Entry date
- Exit date

Relevant products include:

- Daily Vehicle Entry
- Overnight Parking
- Multi-Day Parking

## Rentals

MVP stores simple rental configuration.

Do not build detailed physical-equipment serial-number tracking yet.

---

# 52. Admin Dashboard Rules

Admin dashboard reservation counts must correspond to the four core categories.

Example:

```text
RESORT_ACCESS → Resort Entry card
LIFT_TICKET   → Lift Tickets card
LESSON        → Lessons card
RENTAL        → Rentals card
```

Clicking a card filters booking items by category and displays associated:

- Booking
- Customer
- Product
- Category-specific details
- Payment
- Status

---

# 53. Product Management Considerations

Admins should eventually be able to:

- Add product
- Edit product
- Change price
- Change description
- Change image
- Activate/deactivate product

For historical safety:

Prefer deactivation:

```text
is_active = false
```

over deleting products that already appear in bookings.

---

# 54. MVP Scope

The MVP must support:

```text
Customer

✓ Browse Resort Entry
✓ Browse Lift Tickets
✓ Browse Lessons
✓ Browse Rentals
✓ Product detail
✓ Add to cart
✓ Anonymous cart
✓ Cart editing
✓ Login
✓ Registration
✓ Checkout
✓ Stripe payment
✓ Booking confirmation
✓ My Bookings


Admin

✓ Admin login
✓ Dashboard
✓ Resort Entry reservation count
✓ Lift Ticket reservation count
✓ Lesson reservation count
✓ Rental reservation count
✓ Category reservation lists
✓ Booking/reservation details
✓ Basic product management
✓ Lesson session management


Technical

✓ Next.js frontend
✓ Spring Boot backend
✓ PostgreSQL
✓ Authentication
✓ Role authorization
✓ Git/GitHub
✓ Deployment
✓ Purchased domain
✓ HTTPS
```

---

# 55. Features Explicitly Excluded from MVP

Do NOT implement these unless the user later requests them:

```text
AI recommendation
Chat
Social features
Loyalty points
Native mobile app
Complex coupon system
Multi-language
Dynamic package builder
Resort partner dashboard
Instructor accounts
Hotel booking
Flight booking
Live weather integration
Complex rental inventory tracking
Equipment serial-number management
Advanced refund subsystem
```

The MVP goal is:

```text
Browse
→ Cart
→ Login/Register
→ Checkout
→ Pay
→ Booking
→ Admin View
```

---

# 56. Development Roadmap

## Phase 0 — Planning

Already defined:

- Customer scope
- Admin scope
- Product categories
- Customer flow
- Admin flow
- Initial database ERD
- Initial REST API
- Backend structure
- Frontend structure
- Technology stack

---

## Phase 1 — Brand and Domain

Tasks:

1. Choose final product name
2. Search domain availability
3. Purchase domain
4. Define basic visual identity
5. Decide final URL structure

Do not block development if branding is not final.

Use a temporary internal name if necessary.

---

## Phase 2 — Repository Setup

Create:

```text
ski-booking-platform/
frontend/
backend/
docs/
PROJECT_CONTEXT.md
README.md
```

Set up Git and GitHub.

---

## Phase 3 — Backend Skeleton

Create Spring Boot application with dependencies such as:

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Security
- Validation
- JWT-related library if selected
- Lombok only if intentionally desired

Set up:

- Environment config
- Database connection
- Entities
- Repositories
- Exception handling

---

## Phase 4 — Database

Implement:

- Migrations/schema
- Enums
- Relationships
- Seed/test data

Prefer migration tooling such as Flyway or Liquibase instead of relying on destructive automatic schema recreation for production.

---

## Phase 5 — Authentication

Implement:

- Register
- Login
- Password hashing
- JWT/session strategy
- Current-user endpoint
- CUSTOMER / ADMIN authorization

---

## Phase 6 — Product Browsing

Implement:

- Resorts
- Product lists
- Category filtering
- Product detail
- Lesson session availability

---

## Phase 7 — Shopping Cart

Implement:

- Anonymous cart
- Cart items
- Category-specific item data
- Cart modification
- Cart persistence
- Cart/user merge on authentication

---

## Phase 8 — Booking

Implement:

- Checkout validation
- Booking creation
- Booking item snapshot
- Total calculation
- User booking history

---

## Phase 9 — Stripe

Implement:

- Payment intent/session
- Confirmation
- Success/failure handling
- Booking confirmation state
- Secure payment reference storage

---

## Phase 10 — Admin

Implement:

- Admin dashboard
- Category counts
- Category reservation list
- Reservation detail
- Product management
- Lesson session management

---

## Phase 11 — Frontend Polish

Implement:

- Responsive layout
- Loading states
- Empty states
- Validation
- Error handling
- Sold-out states
- Success screens
- Consistent UI

---

## Phase 12 — Deployment

Deploy:

```text
Frontend → Vercel
Backend → AWS
Database → AWS RDS PostgreSQL
Images → AWS S3
Domain → purchased custom domain
HTTPS → enabled
```

---

## Phase 13 — Testing

Required test areas:

- Authentication
- Authorization
- Cart
- Checkout
- Pricing
- Lesson capacity
- Payment
- Admin access
- Booking history
- Resort access details

---

# 57. Recommended Implementation Order for Codex

Codex should NOT generate the entire product in one giant step.

Recommended sequence:

```text
1. Initialize repository
2. Create backend skeleton
3. Configure PostgreSQL
4. Implement enums/entities
5. Implement repositories
6. Add DTOs
7. Implement auth/security
8. Implement public product APIs
9. Implement lesson sessions
10. Implement cart
11. Implement booking
12. Integrate Stripe
13. Implement admin APIs
14. Create Next.js frontend
15. Implement customer pages
16. Connect frontend APIs
17. Implement checkout/auth flow
18. Implement admin frontend
19. Add tests
20. Deploy
```

At each stage:

- Keep code compiling
- Keep commits small
- Do not invent requirements that conflict with this document
- Explain architectural changes before making major deviations

---

# 58. Implementation Assumptions

Unless later changed by the user:

1. One Git repository contains frontend and backend.
2. Next.js App Router is used.
3. Spring Boot exposes REST APIs.
4. PostgreSQL is the primary database.
5. Stripe is used for payment.
6. Customers may add items anonymously.
7. Checkout requires login/register.
8. Admin is a separate role.
9. Admin uses `/admin` routes in the same frontend app for MVP.
10. Four fixed product categories are used.
11. Resort Entry means vehicle/resort access and parking.
12. `resorts` table represents actual ski resorts.
13. Lesson sessions have capacity.
14. Historical bookings preserve price snapshots.
15. Rental inventory is intentionally simplified in MVP.
16. Production deployment will use a purchased domain and HTTPS.

---

# 59. Areas Still To Be Finalized

These are not yet locked and should be discussed before implementation if needed.

## Branding

Still need:

- Final website name
- Logo
- Domain name

## Exact Resorts

Need to decide whether to:

- Use fictional resorts for development
- Use real Australian resorts for demonstration
- Support one resort first
- Support multiple resorts from day one

The database already supports multiple resorts.

## Resort Access Pricing Logic

Need to define whether:

- Daily tickets are fixed price
- Overnight parking depends on number of nights
- Multi-day parking has fixed package pricing
- Dates affect pricing

## Lift Ticket Availability

Current design defines booking dates but has not yet added a dedicated lift-ticket availability/capacity table.

For MVP, lift tickets may be treated as non-capacity-limited unless inventory limits are required.

## Rental Availability

Current MVP intentionally simplifies rental inventory.

If real stock control becomes required, introduce a dedicated inventory model later.

## Cancellation / Refund Rules

Basic statuses exist, but the following have not yet been defined:

- Cancellation deadline
- Refund percentage
- Partial refunds
- Refund eligibility
- Cancellation policy

## Email

Booking confirmation email is desirable but is not yet required for core MVP.

## Taxes / Fees

No GST/service-fee model has been finalized yet.

This should be defined before production payment calculations.

---

# 60. Guidance for Codex

When working on this project:

- Treat this file as the current product specification.
- Do not silently redefine `Resort Entry`.
- Do not confuse `RESORT_ACCESS` products with the `resorts` entity.
- Keep customer and admin requirements aligned with the four categories.
- Do not require login before Add to Cart.
- Preserve anonymous carts during authentication.
- Do not trust client-calculated pricing.
- Preserve booking price snapshots.
- Enforce admin security at backend level.
- Enforce lesson capacity at backend level.
- Keep the MVP focused.
- Avoid adding speculative features.
- Prefer maintainable, readable code over excessive abstraction.
- Keep frontend and backend contracts explicit with DTOs and TypeScript types.
- Document important architectural decisions.
- Keep secrets out of Git.
- Use environment variables for credentials and API keys.
- Do not commit Stripe secrets, JWT secrets, database passwords, or AWS keys.
- Use appropriate database migrations.
- Add validation and error responses rather than relying on frontend validation alone.

---

# 61. Immediate Next Step

The recommended next implementation task is:

```text
Create the project repository skeleton and initialize the Spring Boot backend and Next.js frontend without implementing full business logic yet.
```

Then implement the backend data model based on the ERD in this document.

Before coding complex payment, finalize:

- Real vs fictional resort seed data
- Exact pricing rules
- GST/service fee behavior
- Cancellation/refund rules

The core MVP architecture and business flow are already defined and should not need redesign before starting development.

---

# 62. First Prompt to Give Codex

After saving this file as:

```text
PROJECT_CONTEXT.md
```

give Codex this instruction:

```text
Read PROJECT_CONTEXT.md completely before making any changes.

Treat PROJECT_CONTEXT.md as the authoritative specification for this project.

Do not start implementing the entire application at once.

First:

1. Review the architecture and requirements.
2. Identify any technical conflicts or missing decisions that would block Phase 2 or Phase 3.
3. Propose the initial monorepo folder structure.
4. Propose the Spring Boot dependencies.
5. Propose the Next.js initialization configuration.
6. Propose the PostgreSQL development setup.
7. Give me a milestone-by-milestone implementation plan.

Do not write application code until I approve the setup plan.
```