# ─────────────────────────────────────────────────────────────
# STAGE 1 — BUILD
# ─────────────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

# Couche de cache : dépendances installées avant la copie du code source
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npx ng build --configuration production

# ─────────────────────────────────────────────────────────────
# STAGE 2 — RUNTIME
# ─────────────────────────────────────────────────────────────
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Builder "application" d'Angular : sortie dans <outputPath>/browser
COPY --from=build /app/dist/frontend-auth/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
