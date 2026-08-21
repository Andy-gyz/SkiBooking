# Snow Alpine Resort frontend

The customer-facing web application uses Next.js 16 App Router, React,
TypeScript, and Tailwind CSS 4.

## Run locally

Start the Spring Boot API on port `8080`, then run:

```bash
set -a
source ../.env
set +a
npm run dev
```

Open `http://localhost:3000`.

The API address defaults to `http://localhost:8080`. To override it, create a
local environment file:

```bash
cp .env.example .env.local
```

## Customer routes

- `/` — Snow Alpine Resort home page
- `/resort-entry` — active resort-access products
- `/lift-tickets` — active lift-ticket products
- `/lessons` — active lesson products
- `/rentals` — active rental products
- `/cart` — anonymous cart review and quantity management
- `/login` — customer sign in with cart preservation
- `/register` — customer account creation with cart preservation
- `/account` — customer profile, active-cart action and booking history
- `/checkout` — authenticated booking creation and Stripe test payment
- `/booking-confirmation/[bookingNumber]` — server-verified payment receipt
- `/bookings/[bookingNumber]` — protected customer booking detail
- `/admin` — role-protected reservation operations dashboard
- `/admin/reservations/[category]` — confirmed category reservation list
- `/admin/bookings/[id]` — complete administrator booking and payment record

Category pages are server-rendered from the public Spring Boot catalog API and
include loading, empty, and backend-unavailable states. Each product category
has its own booking fields, including vehicle details, lift dates, live lesson
session capacity, and rental sizing.

## Milestone 10 cart

Customers can browse and add products without creating an account. The first
add creates an anonymous backend cart; its cart ID and access token are stored
in browser local storage so the cart survives navigation and page reloads.

The cart page displays backend-confirmed prices and configuration details and
supports quantity changes and item removal.

## Milestone 11 authentication

Registration first sends a six-digit, time-limited email code through Resend.
After verification, registration and login pass the anonymous cart token to the backend, which
claims or merges that cart into the authenticated customer's active cart. The
frontend then switches all cart operations from the anonymous capability token
to the customer's Bearer JWT. Account and cart identity survive page reloads,
and logout clears the local customer session.

Checkout requires authentication and reviews customer details, selected items,
and backend-confirmed totals.

## Milestone 12 payments

Continuing from checkout creates a pending backend booking and an idempotent
Stripe PaymentIntent, then renders Stripe Payment Element without passing raw
card data through Snow Alpine. Pending booking numbers are retained locally so
an interrupted payment resumes after a refresh instead of creating another
order. After Stripe confirmation, the frontend asks the backend to reconcile
the PaymentIntent and displays a confirmed booking receipt.

The frontend process must receive `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` from the root `.env`
file. Use Stripe test card `4242 4242 4242 4242`, any future expiry, and any
three-digit CVC for a local successful-payment test.

## Milestone 13 My Bookings

The account page loads the signed-in customer's booking history newest first,
including booking status, reference, date, item count, and total. Each row opens
a protected detail route with immutable item snapshots, booking-specific dates
and configuration, guest information, and the trusted total. Pending bookings
can be handed back to checkout to resume their existing Stripe PaymentIntent.

## Milestone 14 admin operations

Administrators are routed to a dedicated Snow Alpine Operations dashboard after
sign-in. Dashboard counts include only confirmed or completed purchased
quantities. Each of the four booking categories opens a reservation table with
guest snapshots, category-specific selections, totals, and payment state. An
administrator can then open the internal booking record to inspect every item
and Stripe payment attempt. Frontend role states improve navigation, while the
Spring Security `ADMIN` requirement remains the authoritative access control.

## Milestone 15 inventory management

The administrator workspace now includes `/admin/products` for creating,
editing, activating, and safely deactivating bookable products. Historical
booking snapshots remain intact when a product is deactivated. The
`/admin/lesson-sessions` route manages lesson dates, times, status, and total
capacity, shows booked and available places, and prevents capacity from being
reduced below the number of existing reservations. Its schedule generator can
create up to 63 days in one action using selected weekdays and reusable daily
time slots; existing matching sessions are skipped without being overwritten.

## Milestone 16 production readiness

The customer app defaults to `https://snowalpineresort.com` and
`https://api.snowalpineresort.com` in production, publishes canonical Open
Graph metadata, a public-route sitemap and crawler exclusions for account,
checkout, booking and administrator routes. Unexpected render failures use
branded recoverable error screens, common security headers are added to every
response, and standalone output supports a non-root production container.

## Visual direction

The interface uses a product-led alpine design system: oversized high-contrast
sans-serif type, generous white space, snow-white and near-black section
transitions, pill controls, soft translucent booking surfaces, and vivid
category-specific gradients. The visual language is inspired by premium travel
software while remaining specific to the Snow Alpine booking experience.

## Quality checks

```bash
npm run lint
npm run build
```
