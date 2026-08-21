# Architecture

Snow Alpine is a monorepo containing a Next.js storefront and administrator
workspace, a stateless Spring Boot REST API, and PostgreSQL persistence.

```text
Next.js / Vercel
  ├─ public catalog and product configuration
  ├─ anonymous cart and customer account UI
  ├─ Stripe Payment Element
  └─ role-aware administrator workspace
             │ HTTPS JSON + Bearer JWT / cart capability token
             ▼
Spring Boot / AWS App Runner
  ├─ catalog, cart, booking and payment services
  ├─ verified-email authentication
  ├─ administrator inventory and capacity controls
  ├─ Stripe webhook verification
  └─ Flyway migrations
             │ private VPC + TLS
             ▼
Amazon RDS for PostgreSQL
```

The API is stateless and may be replaced without losing customer state. Durable
state lives in PostgreSQL and Stripe. Product prices and booking details are
trusted only when calculated or snapshotted by the backend.

The local profile seeds rolling lesson sessions for development. Flyway seeds
the initial resort and sixteen-product catalog in every new database, while
production lesson sessions remain an explicit administrator operation.

Production domains, networking, secrets, health checks and launch gates are
documented in [`deployment/README.md`](deployment/README.md).
