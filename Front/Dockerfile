# Stage 1: build Vue app
FROM node:20.12.2-alpine3.19 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent || npm i --silent
COPY . .
RUN npm run build

# Stage 2: serve with nginx
FROM nginx:1.25.5-alpine
WORKDIR /usr/share/nginx/html
COPY --from=build /app/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]