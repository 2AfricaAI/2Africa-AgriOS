# ============================================================================
# Frontend image — multi-stage build
#   Stage 1: vite production build
#   Stage 2: nginx serving static files + API reverse proxy
# ============================================================================

FROM node:20-alpine AS builder
WORKDIR /build

# Speed up npm + retry on flaky networks
ENV NPM_CONFIG_FETCH_RETRIES=10 \
    NPM_CONFIG_FETCH_RETRY_FACTOR=3

# Only copy package files first to maximize cache hit
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

# Copy source and build
COPY . .

# Vite picks up VITE_* env vars at build time
ARG VITE_PUBLIC_BASE_URL=
ENV VITE_PUBLIC_BASE_URL=${VITE_PUBLIC_BASE_URL}

RUN npm run build

# ============================================================================
FROM nginx:1.27-alpine AS runtime

# Drop default config; ours wires /api → backend and serves the SPA fallback.
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/agrios.conf

# Built assets
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost/healthz || exit 1
