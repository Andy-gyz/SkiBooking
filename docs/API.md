# REST API

The backend serves JSON under `/api`. Authentication uses a Bearer access token:

```http
Authorization: Bearer <accessToken>
```

## Authentication

### Register

First request a six-digit email verification code:

```http
POST /api/auth/verification-codes
```

```json
{
  "email": "andy@example.com"
}
```

Codes expire after 10 minutes, can be resent after 60 seconds, are stored only
as password-style hashes, and allow at most five incorrect attempts. A repeated
request during the cooldown returns `429 VERIFICATION_CODE_COOLDOWN`.

`POST /api/auth/register` returns `201 Created`.

```json
{
  "firstName": "Andy",
  "lastName": "Example",
  "email": "andy@example.com",
  "password": "a-password-with-at-least-8-characters",
  "verificationCode": "123456",
  "phone": "+61 400 000 000",
  "cartToken": "<optional-anonymous-cart-token>"
}
```

Email addresses are stored in lowercase. Registration always creates a
`CUSTOMER`; clients cannot select their own role. Registration requires the
latest valid verification code for that email. Invalid, expired, or exhausted
codes return `400 INVALID_VERIFICATION_CODE`.

### Login

`POST /api/auth/login` returns `200 OK`.

```json
{
  "email": "andy@example.com",
  "password": "a-password-with-at-least-8-characters",
  "cartToken": "<optional-anonymous-cart-token>"
}
```

Register and login return the same authentication response:

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "firstName": "Andy",
    "lastName": "Example",
    "email": "andy@example.com",
    "phone": "+61 400 000 000",
    "role": "CUSTOMER"
  },
  "cartId": 12
}
```

When `cartToken` is supplied, authentication claims that anonymous cart. If the
user already has an active cart, its items are merged and `cartId` identifies
the surviving cart. `cartId` is `null` when no cart exists.

### Current user

`GET /api/auth/me` requires a valid token and returns the `user` object above.

### Logout

`POST /api/auth/logout` requires a valid token and returns `204 No Content`.
Authentication is stateless, so the client completes logout by deleting its
token. The token is not server-side revoked in V1.

## Authorization

- `/api/auth/me` and `/api/auth/logout` require authentication.
- `/api/bookings/**` and `/api/my-bookings` require authentication.
- `/api/admin/**` requires the JWT role `ADMIN`.
- Product browsing and shopping-cart routes remain public; each cart operation
  still applies its own JWT or capability-token ownership check.

## Errors

All validation and authentication failures use this shape:

```json
{
  "timestamp": "2026-08-20T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/api/auth/register",
  "fieldErrors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

Current error codes include `VALIDATION_FAILED`, `MALFORMED_REQUEST`,
`EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`, and `FORBIDDEN`.

## Public catalog

These endpoints do not require authentication. They expose only active resorts,
enabled products belonging to active resorts, and active lesson sessions.

### Resorts

```http
GET /api/resorts
GET /api/resorts/{id}
```

A resort response contains `id`, `name`, `location`, `description`, and
`imageUrl`. Inactive or unknown resort IDs return `404 RESOURCE_NOT_FOUND`.

### Products

```http
GET /api/products
GET /api/products?category=RESORT_ACCESS
GET /api/products?category=LIFT_TICKET
GET /api/products?category=LESSON
GET /api/products?category=RENTAL
GET /api/products/{id}
```

Each product includes a resort summary, category, description, trusted price,
`AUD` currency, and image URL. An invalid category returns
`400 INVALID_PARAMETER`.

### Lesson availability

```http
GET /api/lesson-sessions?productId=3&date=2026-08-25
```

Sessions are ordered by start time. Each response includes `capacity` and
`availableCount`; cancelled sessions are excluded. The product must be an
available `LESSON` product, otherwise the API returns
`400 INVALID_CATALOG_REQUEST` or `404 RESOURCE_NOT_FOUND`.

## Shopping carts

### Create or retrieve an active cart

```http
POST /api/carts
```

For an anonymous request, this creates a cart and returns a 256-bit capability
token exactly once:

```json
{
  "cartToken": "<43-character-base64url-token>",
  "cart": {
    "id": 12,
    "status": "ACTIVE",
    "itemCount": 0,
    "subtotal": 0,
    "total": 0,
    "currency": "AUD",
    "items": []
  }
}
```

The frontend must persist both `cart.id` and `cartToken`. Every anonymous read or
mutation sends:

```http
X-Cart-Token: <cartToken>
```

An authenticated request reuses the user's current active cart or creates one.
Its `cartToken` response field is `null`; access is controlled by the Bearer JWT.

### Read and modify

```http
GET    /api/carts/{cartId}
POST   /api/carts/{cartId}/items
PUT    /api/carts/{cartId}/items/{itemId}
DELETE /api/carts/{cartId}/items/{itemId}
```

The backend reads current product prices and calculates each subtotal and total.
Client-supplied prices are not part of the request contract.

Category-specific create examples:

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

```json
{
  "productId": 2,
  "quantity": 2,
  "bookingDate": "2026-08-25"
}
```

```json
{
  "productId": 3,
  "quantity": 1,
  "lessonSessionId": 21
}
```

```json
{
  "productId": 4,
  "quantity": 1,
  "rentalStartDate": "2026-08-25",
  "rentalEndDate": "2026-08-26",
  "rentalSize": "Adult Medium",
  "rentalBootSize": "AU 9"
}
```

`PUT` keeps the item's existing product and accepts the same category-specific
fields except `productId`. Quantity is limited to 1–20. Lesson availability is
checked when the item is added or updated and will be checked again at checkout.

Cart errors include `CART_NOT_FOUND`, `CART_ITEM_NOT_FOUND`,
`INVALID_CART_ITEM`, and `INSUFFICIENT_LESSON_CAPACITY`. A missing or incorrect
cart token deliberately returns the same `404 CART_NOT_FOUND` response.

## Bookings

All booking endpoints require a Bearer JWT. A customer can only check out their
own active cart and read their own bookings.

### Create from an active cart

```http
POST /api/bookings
```

```json
{
  "cartId": 12,
  "firstName": "Andy",
  "lastName": "Example",
  "email": "andy@example.com",
  "phone": "+61 400 000 000"
}
```

A successful request returns `201 Created`. The backend revalidates every item,
uses current product prices, creates immutable item snapshots, marks the cart
`CHECKED_OUT`, and returns a `PENDING` booking:

```json
{
  "bookingNumber": "SKI-20260820-12AB34CD56EF",
  "status": "PENDING",
  "currency": "AUD",
  "totalAmount": 270.00,
  "customerFirstName": "Andy",
  "customerLastName": "Example",
  "customerEmail": "andy@example.com",
  "customerPhone": "+61 400 000 000",
  "createdAt": "2026-08-20T00:00:00Z",
  "items": [
    {
      "id": 31,
      "productId": 2,
      "lessonSessionId": null,
      "productName": "Full Day Lift Pass",
      "category": "LIFT_TICKET",
      "quantity": 2,
      "unitPrice": 135.00,
      "subtotal": 270.00,
      "bookingDate": "2026-08-25",
      "vehicleRegistration": null,
      "vehicleType": null,
      "entryDate": null,
      "exitDate": null,
      "rentalStartDate": null,
      "rentalEndDate": null,
      "rentalSize": null,
      "rentalBootSize": null
    }
  ]
}
```

Lesson rows are locked during checkout and capacity is reserved in the same
database transaction. Concurrent checkout cannot exceed the session capacity.
Payment confirmation will move a booking from `PENDING` to `CONFIRMED` in the
payment milestone; failed or expired payment capacity release belongs to that
workflow.

Checkout errors include `CART_NOT_FOUND`, `EMPTY_CART`, `INVALID_CHECKOUT`, and
`INSUFFICIENT_LESSON_CAPACITY`.

### Booking history and detail

```http
GET /api/my-bookings
GET /api/bookings/{bookingNumber}
```

History is newest first and returns booking number, status, currency, total,
total purchased quantity as `itemCount`, and creation time. Detail returns the
full snapshot shape shown above. An unknown booking or a booking belonging to a
different customer returns `404 BOOKING_NOT_FOUND`.

## Payments

Payment creation and confirmation require a Bearer JWT and enforce booking
ownership. The webhook endpoint is public at the HTTP layer but accepts only a
payload with a valid Stripe signature.

### Create or resume payment

```http
POST /api/payments/create
```

```json
{
  "bookingNumber": "SKI-20260820-12AB34CD56EF"
}
```

The response contains the Stripe client secret needed by Stripe.js:

```json
{
  "bookingNumber": "SKI-20260820-12AB34CD56EF",
  "bookingStatus": "PENDING",
  "paymentIntentId": "pi_123",
  "paymentStatus": "PENDING",
  "amount": 270.00,
  "currency": "AUD",
  "clientSecret": "pi_123_secret_..."
}
```

The backend converts the trusted booking total to AUD cents and uses the booking
number as a Stripe idempotency key. Repeating the request reuses the existing
PaymentIntent instead of creating another charge attempt. Only the authenticated
booking owner receives its client secret.

### Reconcile payment status

```http
POST /api/payments/confirm
```

The request uses the same `bookingNumber` body. This endpoint does not trust a
status supplied by the browser; it retrieves the PaymentIntent from Stripe and
updates the local payment and booking from that result. The response uses the
shape above but omits `clientSecret`.

### Stripe webhook

```http
POST /api/payments/webhook
Stripe-Signature: <stripe-signature>
```

The endpoint verifies the signature against the unmodified request body and
handles:

- `payment_intent.processing`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`
- `payment_intent.canceled`

A successful event changes the payment to `SUCCEEDED` and booking to
`CONFIRMED`. A failed payment attempt changes only the payment to `FAILED`; the
booking remains `PENDING` so the customer can retry with another card. A
definitively cancelled PaymentIntent changes the booking to `CANCELLED` and
releases its lesson capacity exactly once. Duplicate webhook delivery is safe.

Payment errors include `PAYMENT_NOT_FOUND`, `INVALID_PAYMENT`,
`INVALID_STRIPE_WEBHOOK`, and `STRIPE_UNAVAILABLE`.

## Administration

Every `/api/admin/**` endpoint requires a Bearer JWT with role `ADMIN`. A valid
customer token returns `403 FORBIDDEN`; authentication is enforced by the backend
and does not depend on hiding frontend routes.

### Dashboard and reservations

```http
GET /api/admin/dashboard
GET /api/admin/bookings?category=RESORT_ACCESS
GET /api/admin/bookings?category=LIFT_TICKET
GET /api/admin/bookings?category=LESSON
GET /api/admin/bookings?category=RENTAL
GET /api/admin/bookings/{id}
```

Dashboard counts sum purchased quantities from booking items whose booking is
`CONFIRMED` or `COMPLETED`. `PENDING` and `CANCELLED` bookings are excluded. The
response shape is:

```json
{
  "resortAccessReservations": 126,
  "liftTicketReservations": 94,
  "lessonReservations": 37,
  "rentalReservations": 68
}
```

The category list returns one row per matching booking item, newest booking
first. Each row includes booking status, customer snapshot, latest payment
status, product snapshot, and all category-specific selections. Booking detail
returns every item and payment attempt for the requested internal booking ID.

### Product management

```http
GET    /api/admin/products
GET    /api/admin/products?category=LESSON
POST   /api/admin/products
PUT    /api/admin/products/{id}
DELETE /api/admin/products/{id}
```

Create and update bodies use:

```json
{
  "resortId": 1,
  "name": "Child Lift Pass",
  "category": "LIFT_TICKET",
  "description": "Full-day lift access for children.",
  "price": 85.00,
  "imageUrl": "https://example.com/child-pass.jpg",
  "active": true
}
```

`DELETE` returns `204 No Content` and sets `active=false`; it never physically
deletes a product referenced by carts or historical bookings. The admin list
includes inactive products, while public catalog endpoints continue to exclude
them. A product with lesson sessions cannot change category.

### Lesson-session management

```http
GET  /api/admin/lesson-sessions
GET  /api/admin/lesson-sessions?productId=3
POST /api/admin/lesson-sessions
PUT  /api/admin/lesson-sessions/{id}
```

```json
{
  "productId": 3,
  "date": "2026-08-25",
  "startTime": "09:00",
  "endTime": "11:00",
  "capacity": 8,
  "status": "ACTIVE"
}
```

The product must have category `LESSON`, end time must follow start time, and a
product cannot have duplicate date/start slots. Capacity cannot fall below
`bookedCount`. Once a session has bookings, its product, date, times, and status
cannot be destructively changed; increasing capacity remains allowed.

Admin business validation errors return `400 INVALID_ADMIN_REQUEST`.
