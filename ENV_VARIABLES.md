# Environment Variables

This document lists all environment variables used in the PingPad project.

## ⚠️ Important Note

**If you're using Docker Compose (recommended):** You don't need a `.env` file! All environment variables are already configured in `docker-compose.yml`. This document is only needed if you want to:
- Customize the default values
- Run services manually without Docker Compose
- Set up production deployment

## Backend Environment Variables

### Database Configuration
- **`DB_HOST`** (default: `localhost`)
  - PostgreSQL database host
  - Used in: `spring.datasource.url`

- **`DB_PORT`** (default: `5432`)
  - PostgreSQL database port
  - Used in: `spring.datasource.url`

- **`DB_NAME`** (default: `ping-pad-db`)
  - PostgreSQL database name
  - Used in: `spring.datasource.url`

- **`DB_USER`** (default: `postgres`)
  - PostgreSQL database username
  - Used in: `spring.datasource.username`

- **`DB_PASSWORD`** (default: empty)
  - PostgreSQL database password
  - Used in: `spring.datasource.password`

### Redis Configuration
- **`REDIS_HOST`** (default: `localhost`)
  - Redis server host
  - Maps to: `spring.data.redis.host`

- **`REDIS_PORT`** (default: `6379`)
  - Redis server port
  - Maps to: `spring.data.redis.port`

### JWT Configuration
- **`JWT_SECRET`** (default: hardcoded in application.properties)
  - JWT secret key for token signing
  - Must be at least 32 characters for HS256 algorithm
  - **IMPORTANT**: Change this in production!

- **`JWT_EXPIRATION`** (default: `86400000` = 24 hours)
  - JWT token expiration time in milliseconds

### API Testing Engine Configuration
- **`API_TESTING_ENGINE_URL`** (default: `http://api-testing-engine:8081`)
  - URL of the Go API Testing Engine service
  - For Docker: `http://api-testing-engine:8081`
  - For local development: `http://localhost:8081`

### Spring Boot Configuration
- **`SPRING_PROFILES_ACTIVE`** (default: not set)
  - Active Spring profile (e.g., `docker`, `dev`, `prod`)
  - When set to `docker`, uses `application-docker.yml` configuration

## Frontend Environment Variables

### Backend URL
- **`VITE_BACKEND_URL`** (default: `http://localhost:8080`)
  - Backend API URL for the frontend to connect to
  - For Docker: `http://localhost:8080` (or your backend URL)
  - For local development: `http://localhost:8080`

## API Testing Engine Environment Variables (Go Service)

These are used by the Go API Testing Engine service:

- **`SERVER_HOST`** (default: `0.0.0.0`)
  - Server bind address

- **`SERVER_PORT`** (default: `8081`)
  - Server port

- **`TESTING_MAX_CONCURRENCY`** (default: `100`)
  - Maximum concurrent test requests

- **`TESTING_RATE_LIMIT`** (default: `100`)
  - Rate limit for test requests

- **`TESTING_DEFAULT_TIMEOUT`** (default: `30s`)
  - Default timeout for API tests

- **`TESTING_MAX_RETRIES`** (default: `3`)
  - Maximum retry attempts for failed tests

## Example .env File

Create a `.env` file in the project root with the following variables:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ping-pad-db
DB_USER=postgres
DB_PASSWORD=your_password_here

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-must-be-at-least-32-characters-long-for-hs256-algorithm-to-work-properly
JWT_EXPIRATION=86400000

# API Testing Engine Configuration
API_TESTING_ENGINE_URL=http://api-testing-engine:8081

# Frontend Configuration
VITE_BACKEND_URL=http://localhost:8080

# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=docker
```

## Docker Compose

When using Docker Compose, most environment variables are set in the `docker-compose.yml` file. However, you can override them by:

1. Creating a `.env` file in the project root
2. Docker Compose will automatically load variables from `.env`
3. You can also set them directly in `docker-compose.yml` under the `environment` section

## Notes

- The backend uses `spring.config.import=optional:file:.env[.properties]` to load environment variables from a `.env` file
- Frontend environment variables must be prefixed with `VITE_` to be accessible in the browser
- For production, ensure all sensitive values (especially `JWT_SECRET` and `DB_PASSWORD`) are properly secured
- Default values are shown in parentheses - these are used if the environment variable is not set
