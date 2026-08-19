# ADR 0002: Stateless JWT authentication

- Status: Accepted
- Date: 2026-08-20

## Context

The customer frontend and administrator frontend need one API authentication
mechanism. Customers must remain anonymous while browsing and building a cart,
but checkout and account routes require identity. Administrator authorization
must be enforced by the backend.

## Decision

- The API issues HMAC-SHA256 signed JWT access tokens after registration or
  login.
- JWT signing material comes only from the `JWT_SECRET` environment variable and
  must contain at least 32 characters.
- Access tokens expire after one hour by default. The issuer and duration remain
  environment-configurable.
- Passwords are stored using BCrypt and are never included in API DTOs.
- Self-registration always assigns `CUSTOMER`; administrator roles cannot be
  requested by a public client.
- Spring Security validates the token signature, issuer, expiry, and role claim
  on every protected request without creating an HTTP session.
- `/api/admin/**` requires `ADMIN` at the filter-chain level.
- Logout is client-side token deletion and returns `204 No Content` after token
  validation.
- Authentication and validation failures use a stable JSON error contract.

## Consequences

The API remains horizontally scalable because it stores no login session. V1
cannot revoke one access token before expiry; short token lifetime limits that
window. Refresh tokens, token revocation, password reset, email verification,
and administrator provisioning are intentionally deferred until their product
flows are designed.
