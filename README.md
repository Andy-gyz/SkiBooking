# Ski Booking Platform

A production-style full-stack ski booking platform for resort access, lift
tickets, lessons, and rentals.

## Repository layout

- `frontend/` — Next.js customer and admin web application
- `backend/` — Spring Boot REST API
- `docs/` — architecture, API, ERD, and decision records
- `compose.yaml` — local PostgreSQL development service
- `PROJECT_CONTEXT.md` — authoritative product specification

## Prerequisites

- Java 25
- Node.js 24 and npm
- Docker Desktop with Docker Compose

## Local development

Create your local environment file:

```bash
cp .env.example .env
```

Replace `JWT_SECRET` in `.env` with a private random value of at least 32
characters. One way to generate one is `openssl rand -base64 48`.

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the backend:

```bash
cd backend
set -a
source ../.env
set +a
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

For Stripe sandbox payments, add your `sk_test_...` secret key and the local
webhook secret to `.env`. Obtain the webhook secret in another terminal:

```bash
stripe login
stripe listen \
  --events payment_intent.processing,payment_intent.succeeded,payment_intent.payment_failed,payment_intent.canceled \
  --forward-to localhost:8080/api/payments/webhook
```

Copy the printed `whsec_...` value into `STRIPE_WEBHOOK_SECRET`, then restart the
backend so it reads the new environment value. The frontend embeds only the
safe `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY`; the backend secret key is never exposed to the
browser.

Customer registration sends six-digit verification codes through Resend. Set
`RESEND_API_KEY` and quote `EMAIL_FROM` in `.env`. Without a verified custom
domain, use `Snow Alpine <onboarding@resend.dev>` and register with the email
address that owns the Resend account. Resend restricts this testing sender to
that one recipient.

To create a local-only administrator, set `LOCAL_ADMIN_EMAIL` and a private
`LOCAL_ADMIN_PASSWORD` of at least eight characters in `.env`, then restart the
backend with the `local` profile. The account is created only when both values
are configured. An existing customer account is never automatically promoted.

The `local` profile loads fictional development seed data. Omit the profile to
run with the core schema only.

Run the frontend in another terminal:

```bash
cd frontend
set -a
source ../.env
set +a
npm run dev
```

The frontend runs at `http://localhost:3000` and the backend runs at
`http://localhost:8080`.

The customer frontend currently provides these public routes:

```text
/
/resort-entry
/lift-tickets
/lessons
/rentals
```

The category pages render current product and price data from the backend. Set
`NEXT_PUBLIC_API_BASE_URL` in `frontend/.env.local` only when the API is not
available at the default `http://localhost:8080` address.

Run backend tests (Docker Desktop must be running):

```bash
cd backend
./mvnw test
```

Tests use a disposable Testcontainers PostgreSQL instance and do not modify the
local development database.

## Authentication API

The backend exposes stateless Bearer JWT authentication at `/api/auth`. See
[`docs/API.md`](docs/API.md) for requests, responses, and error contracts.

## Current status

Milestone 16 adds production-readiness controls for `snowalpineresort.com`: a
fail-fast Spring production profile, private-database health probes, repeatable
initial catalog and administrator bootstrap, container builds, frontend error
boundaries, canonical metadata, robots and sitemap output, security headers,
GitHub CI, and a deployment runbook for Vercel, App Runner, RDS, Stripe and
Resend. The responsive
storefront now includes category-specific product configuration, a persistent
anonymous cart, verified-email registration and login, secure anonymous-cart
claiming, an account page, authenticated checkout, Stripe Payment Element,
resumable pending payments, server-side payment reconciliation, and a booking
confirmation receipt. Customers can now see every booking in their account,
open its protected itinerary and guest details, and resume pending payment from
the booking detail page. Administrators receive a role-protected dashboard,
confirmed reservation totals for all four categories, category-specific guest
lists, full booking and Stripe payment records, product catalog controls, and
lesson-session capacity management. Local development now starts with four
products in each category and a rolling week of upcoming lesson sessions.

See [`docs/deployment/README.md`](docs/deployment/README.md) for the production
architecture, environment variables, DNS plan and launch gates.
