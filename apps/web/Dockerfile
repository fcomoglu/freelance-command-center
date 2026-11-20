# syntax=docker/dockerfile:1
# Image of the web client, built from the repository root: docker build -f apps/web/Dockerfile -t fcc-web .
# nginx serves the built app and forwards /api to the API (API_URL), so both share one origin as in development.

FROM node:24-alpine AS build
WORKDIR /build
# Every workspace manifest is needed for the lockfile; only the web app and its packages are installed.
COPY package.json package-lock.json ./
COPY apps/web/package.json apps/web/
COPY apps/mobile/package.json apps/mobile/
COPY packages/api-client/package.json packages/api-client/
COPY packages/i18n/package.json packages/i18n/
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund --workspace apps/web --workspace packages/api-client --workspace packages/i18n
COPY packages/ packages/
COPY apps/web/ apps/web/
RUN npm run build --workspace apps/web

FROM nginx:1.29-alpine
ENV API_URL=http://api:8080
COPY apps/web/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /build/apps/web/dist/ /usr/share/nginx/html/
