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

## Milestone 9 routes

- `/` — Snow Alpine Resort home page
- `/resort-entry` — active resort-access products
- `/lift-tickets` — active lift-ticket products
- `/lessons` — active lesson products
- `/rentals` — active rental products

Category pages are server-rendered from the public Spring Boot catalog API and
include loading, empty, and backend-unavailable states. Product configuration
and transactional customer flows are intentionally reserved for later frontend
milestones.

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
