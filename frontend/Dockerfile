# Build Stage
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json ./
RUN npm install

COPY . .
RUN npm run build

# Runtime Stage
FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html

# Template for envsubst
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80

CMD ["/bin/sh", "-c", "envsubst '$BACKEND_URL' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]