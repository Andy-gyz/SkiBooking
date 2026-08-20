# REST API

The backend serves JSON under `/api`. Authentication uses a Bearer access token:

```http
Authorization: Bearer <accessToken>
```

## Authentication

### Register

`POST /api/auth/register` returns `201 Created`.

```json
{
  "firstName": "Andy",
  "lastName": "Example",
  "email": "andy@example.com",
  "password": "a-password-with-at-least-8-characters",
  "phone": "+61 400 000 000",
  "cartToken": "<optional-anonymous-cart-token>"
}
```

Email addresses are stored in lowercase. Registration always creates a
`CUSTOMER`; clients cannot select their own role.

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
- `/api/admin/**` requires the JWT role `ADMIN`.
- Product browsing and future anonymous-cart routes remain public until their
  own route rules are introduced.

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
