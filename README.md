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
backend so it reads the new environment value. The `STRIPE_PUBLISHABLE_KEY` is
reserved for the future frontend; the backend never exposes the secret key.

To create a local-only administrator, set `LOCAL_ADMIN_EMAIL` and a private
`LOCAL_ADMIN_PASSWORD` of at least eight characters in `.env`, then restart the
backend with the `local` profile. The account is created only when both values
are configured. An existing customer account is never automatically promoted.

The `local` profile loads fictional development seed data. Omit the profile to
run with the core schema only.

Run the frontend in another terminal:

```bash
cd frontend
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

Milestone 9 replaces the default Next.js screen with the responsive Snow Alpine
Resort customer experience. It includes the shared header and footer, branded
home page, four public category routes, loading and unavailable states, and
server-rendered catalog integration with the Spring Boot API. Product
configuration, cart, authentication, checkout, and admin screens are the next
frontend slices.
