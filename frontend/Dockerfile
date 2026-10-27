FROM node:22.23.4-alpine3.24 AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:stable-alpine3.24-slim

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/banking-web/browser /usr/share/nginx/html

EXPOSE 80