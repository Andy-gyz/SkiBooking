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

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the backend:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

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

## Current status

Milestone 2 establishes the core PostgreSQL schema, Flyway migrations, JPA
entities, repositories, and isolated persistence tests. Business functionality
will be added incrementally according to `PROJECT_CONTEXT.md`.
