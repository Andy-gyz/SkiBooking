# Snow Alpine Resort frontend

The customer-facing web application uses Next.js 16 App Router, React,
TypeScript, and Tailwind CSS 4.

## Run locally

Start the Spring Boot API on port `8080`, then run:

```bash
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

Category pages are server-rendered from the public Spring Boot catalog API and
include loading, empty, and backend-unavailable states. Each product category
has its own booking fields, including vehicle details, lift dates, live lesson
session capacity, and rental sizing.

## Milestone 10 cart

Customers can browse and add products without creating an account. The first
add creates an anonymous backend cart; its cart ID and access token are stored
in browser local storage so the cart survives navigation and page reloads.

The cart page displays backend-confirmed prices and configuration details and
supports quantity changes and item removal. Sign-in and checkout remain disabled
until Milestone 11.

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
