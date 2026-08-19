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
  "phone": "+61 400 000 000"
}
```

Email addresses are stored in lowercase. Registration always creates a
`CUSTOMER`; clients cannot select their own role.

### Login

`POST /api/auth/login` returns `200 OK`.

```json
{
  "email": "andy@example.com",
  "password": "a-password-with-at-least-8-characters"
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
  }
}
```

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
