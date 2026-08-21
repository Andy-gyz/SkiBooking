# Lightsail portfolio deployment

This deployment runs PostgreSQL, the Spring Boot API, and Caddy on one
2 GB Lightsail instance. It is intended for the public portfolio environment,
not a high-availability commercial production workload.

## Server prerequisites

- Ubuntu 24.04 LTS Lightsail instance
- Static IPv4 attached
- inbound TCP 22, 80, and 443 only
- `api.snowalpineresort.com` A record pointing to the static IPv4
- Docker Engine with the Compose plugin

## Configuration

Create `deploy/lightsail/.env` on the server from `.env.example`. Never commit
that file. Generate independent database, JWT, and administrator passwords.
The repository-level `.gitignore` excludes every `.env` file except examples.

## Start and verify

```bash
cd ~/SkiBooking/deploy/lightsail
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 backend
curl --fail https://api.snowalpineresort.com/actuator/health/readiness
```

The PostgreSQL and backend ports are available only on the private Compose
network. Caddy is the only service published on ports 80 and 443 and obtains
the TLS certificate automatically after DNS resolves.

## Updating

```bash
cd ~/SkiBooking
git pull --ff-only
cd deploy/lightsail
docker compose up -d --build
docker image prune -f
```

## Database backup

Before an application or schema update, create a database dump outside the
container and copy it to a separate encrypted location:

```bash
cd ~/SkiBooking/deploy/lightsail
docker compose exec -T postgres pg_dump -U skibooking -d skibooking -Fc > "$HOME/skibooking-$(date +%F).dump"
```

Never commit database dumps or server `.env` files.
