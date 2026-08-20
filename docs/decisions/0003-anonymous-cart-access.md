# ADR 0003: Capability tokens for anonymous carts

- Status: Accepted
- Date: 2026-08-20

## Context

Customers must browse and build a mixed-category cart before creating an
account. Numeric cart IDs are enumerable and cannot safely authorize access.
After authentication, the anonymous cart must survive and become owned by the
user without allowing another user to access it.

## Decision

- An anonymous cart receives a cryptographically random 256-bit Base64URL token.
- The browser retains the cart ID and token and sends the token in the
  `X-Cart-Token` header for every anonymous cart operation.
- A missing, invalid, inactive, or inaccessible cart returns the same generic
  `404 CART_NOT_FOUND` response to reduce information disclosure.
- Authenticated carts use JWT identity and verify `cart.user_id`; their anonymous
  token is removed.
- Login and registration accept an optional `cartToken`. If no active user cart
  exists, the anonymous cart is claimed in place. Otherwise, its item rows move
  into the existing user cart and the anonymous cart becomes `ABANDONED`.
- The backend obtains unit prices from active products and calculates totals.
- Category-specific data is validated strictly, including lesson/product
  matching and current lesson capacity.
- Checkout will revalidate prices, product availability, dates, and capacity;
  adding an item does not reserve inventory.

## Consequences

Anonymous shopping remains frictionless without exposing carts through guessable
IDs. The token is a bearer capability and must be handled as sensitive browser
state. Cart merging preserves every line rather than trying to infer whether two
configurations are identical. Token hashing and automated abandoned-cart cleanup
can be added later if operational requirements justify them.
