# ---- Build -------------------------------------------------------------------
FROM node:26-alpine AS build
WORKDIR /build

# Dependencies: cached until package.json or the lock file change
COPY package.json package-lock.json ./
RUN npm ci

# Type check + production build into dist/
COPY . .
RUN npm run build

# ---- Run: nginx serves the built files and forwards /api to the backend ------
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80