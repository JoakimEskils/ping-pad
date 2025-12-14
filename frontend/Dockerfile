# Build stage
# FROM node:22-alpine AS build
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json yarn.lock ./
RUN yarn install

COPY . .
RUN yarn build

# Production stage: serve built files with a lightweight server
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80 (default nginx)
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
