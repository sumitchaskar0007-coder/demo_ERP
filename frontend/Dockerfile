# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

COPY . .
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

LABEL org.opencontainers.image.title="Jadhavar ERP Frontend" \
      org.opencontainers.image.description="React frontend and reverse proxy for Jadhavar ERP"

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build --chown=101:101 /workspace/dist /usr/share/nginx/html

USER 101:101
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1

STOPSIGNAL SIGQUIT
