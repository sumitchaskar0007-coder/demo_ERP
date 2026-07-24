# Docker deployment

This repository builds an executable Spring Boot JAR with Java 17 and a Vite
frontend served by unprivileged Nginx. Docker Compose runs PostgreSQL, Redis,
the backend, and the frontend on one host.

## Configure

```bash
cp .env.example .env
```

Replace every `CHANGE_ME` value. Set `PUBLIC_ORIGIN` to the public HTTPS origin,
for example `https://erp.example.com`. Keep `AUTH_COOKIE_SECURE=true` when TLS is
enabled. Bootstrap is disabled by default; if it is enabled for the first
startup, provide all `SUPER_ADMIN_*` values and disable it again afterward.

Generate independent secrets instead of reusing passwords:

```bash
openssl rand -base64 48
openssl rand -base64 48
openssl rand -base64 36
openssl rand -base64 36
```

Use the generated values for `JWT_SECRET`, `RATE_LIMIT_KEY_SECRET`,
`DB_PASSWORD`, and `REDIS_PASSWORD`.

## Build and run

```bash
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=200 backend frontend
```

The public frontend is available at `http://SERVER_IP:8080` unless
`HTTP_PORT` was changed. For production, terminate HTTPS with an Application
Load Balancer, Caddy, Nginx, or another reverse proxy and expose only ports 80
and 443 in the EC2 security group. Do not expose PostgreSQL port 5432 or Redis
port 6379.

Health checks:

- Public container check: `http://SERVER_IP:8080/healthz`
- Internal backend readiness:
  `docker compose exec -T backend wget -qO- http://127.0.0.1:8081/actuator/health/readiness`

## Multi-platform images

For an Apple Silicon development machine and x86_64 EC2 deployment, push a
multi-platform manifest to a registry:

```bash
docker buildx create --name jadhavr-builder --use --bootstrap
docker buildx build --platform linux/amd64,linux/arm64 \
  --build-arg JAVA_VERSION=17 \
  -t REGISTRY/jadhavr-erp-backend:VERSION --push ./backend
docker buildx build --platform linux/amd64,linux/arm64 \
  -t REGISTRY/jadhavr-erp-frontend:VERSION --push ./frontend
```

To test the EC2 architecture locally without pushing:

```bash
docker buildx build --platform linux/amd64 --load \
  --build-arg JAVA_VERSION=17 \
  -t jadhavr-erp-backend:amd64 ./backend
docker buildx build --platform linux/amd64 --load \
  -t jadhavr-erp-frontend:amd64 ./frontend
```

## Amazon Linux 2023

Install Docker Engine and the Docker Compose plugin, enable Docker at boot,
copy the repository and a server-only `.env` file to the instance, then run:

```bash
docker compose config --quiet
docker compose pull
docker compose build
docker compose up -d
docker compose ps
curl --fail http://127.0.0.1:8080/healthz
```

Use an EC2 instance profile for AWS access; do not put long-lived AWS access
keys in `.env`. Back up the `postgres_data` and `uploads_data` volumes. For
high availability or institutional production data, prefer the repository's
existing ECS/RDS/S3 infrastructure over a single-host database.

## Operations

```bash
docker compose logs -f --tail=200
docker compose restart backend
docker compose down
```

`docker compose down` preserves named volumes. Do not add `--volumes` unless
the PostgreSQL, Redis, and upload data is intentionally being deleted.
