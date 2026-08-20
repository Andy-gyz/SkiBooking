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

The `local` profile loads fictional development seed data. Omit the profile to
run with the core schema only.

Run the frontend in another terminal:

```bash
cd frontend
npm run dev
```

The frontend runs at `http://localhost:3000` and the backend runs at
`http://localhost:8080`.

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

Milestone 7 adds Stripe sandbox payments using one idempotent PaymentIntent per
booking. Only signed Stripe webhooks or a server-side Stripe status lookup can
confirm an order. Failed card attempts remain retryable, successful payments are
idempotent, and cancelled PaymentIntents release reserved lesson capacity once.
