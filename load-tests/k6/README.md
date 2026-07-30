# k6 5,000-user test

This suite is intentionally blocked unless `CONFIRM_NON_PRODUCTION=YES` is supplied. Never point it at production.

Create a git-ignored synthetic user file, then run each stage only after the prior stage meets the thresholds:

```bash
k6 run \
  -e BASE_URL=https://isolated-staging.example.com \
  -e USERS_FILE=./users.synthetic.json \
  -e CONFIRM_NON_PRODUCTION=YES \
  5000-users.js
```

Writes are disabled by default. `ENABLE_SAFE_WRITES=true` only enables per-user operations explicitly configured with a safe, idempotent staging path. Source-IP/NAT behavior must be tested from controlled load generators with assigned egress IPs; spoofed forwarding headers are intentionally ignored.
