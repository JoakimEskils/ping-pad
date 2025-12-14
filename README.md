# PingPad

A SaaS tool for testing REST API endpoints and logging webhooks. Built with Spring Boot (backend), React + Vite (frontend), and a Go API testing engine, using a modular monolith architecture.

## Table of Contents

- [Quick Start](#quick-start)
- [Prerequisites](#prerequisites)
- [Local Development](#local-development)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Documentation](#documentation)
- [Testing](#testing)

## Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/ping-pad.git
   cd ping-pad
   ```

2. Start all services:
   ```bash
   docker compose up --build
   ```

3. Access the application:
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080
   - API Testing Engine: http://localhost:8081
   - Nginx Gateway: http://localhost:80

## Prerequisites

- Docker Desktop installed and running
- Git

## Local Development

### Running with Docker Compose

Docker Compose is the recommended way to run the project locally. It automatically configures all services, databases, and networking.

```bash
docker compose up --build
```

This starts:
- PostgreSQL database (port 5432)
- Redis cache (port 6379)
- Spring Boot backend (port 8080)
- React frontend (port 5173)
- Go API Testing Engine (port 8081)
- Nginx reverse proxy (port 80)

### Manual Setup

For manual setup without Docker, see [SETUP.md](SETUP.md) for detailed instructions.

### Environment Variables

Environment variables are pre-configured in `docker-compose.yml`. For custom configuration or manual setup, see [ENV_VARIABLES.md](ENV_VARIABLES.md).

## Project Structure

```
ping-pad/
├── backend/              # Spring Boot application
├── frontend/             # React + Vite application
├── api-testing-engine/   # Go API testing service
├── nginx.conf           # Nginx configuration
└── docker-compose.yml   # Docker Compose configuration
```

## Architecture

PingPad uses a modular monolith architecture with the following components:

- **Frontend**: React application served by Nginx
- **Backend**: Spring Boot application handling business logic and data persistence
- **API Testing Engine**: Go service for high-performance HTTP request execution
- **Database**: PostgreSQL for data storage and event sourcing
- **Cache**: Redis for caching frequently accessed data
- **Gateway**: Nginx reverse proxy for routing and rate limiting

### Communication Flow

1. User initiates API test from React frontend
2. Spring Boot backend retrieves endpoint configuration from database
3. Backend sends test request to Go API Testing Engine via gRPC
4. Go engine executes HTTP request to target API
5. Results are returned to backend, saved to database, and displayed in frontend

For detailed architecture information, see the [Architecture & Data Flow](#architecture--data-flow) section below.

## Documentation

- [SETUP.md](SETUP.md) - Detailed setup instructions and troubleshooting
- [ENV_VARIABLES.md](ENV_VARIABLES.md) - Complete environment variables reference
- [TEST_STRUCTURE.md](TEST_STRUCTURE.md) - Testing strategy and structure
- [EVENT_SOURCING_EXPLANATION.md](EVENT_SOURCING_EXPLANATION.md) - Event sourcing implementation details

## Testing

### Running Tests

**Backend (Java):**
```bash
cd backend
mvn test
```

**API Testing Engine (Go):**
```bash
cd api-testing-engine
go test ./...
```

For detailed testing information, see [TEST_STRUCTURE.md](TEST_STRUCTURE.md).

## Architecture & Data Flow

PingPad follows a modular monolith pattern with clear domain boundaries. The system uses event sourcing for audit trails, Redis for caching, and gRPC for inter-service communication.

### Key Features

- **Modular Monolith**: Organized by business domains (Auth, User Management, API Testing, Event Sourcing, Cache)
- **Event Sourcing**: Complete audit trail and time-travel capabilities using PostgreSQL
- **Caching Layer**: Redis-based hybrid cache-aside and write-through pattern
- **gRPC Communication**: High-performance binary protocol between Java backend and Go testing engine
- **Correlation IDs**: End-to-end request tracing across all services
- **Recurring Testing**: Automatic endpoint testing every 30 seconds with real-time analytics

### Services

- **Backend**: Handles authentication, endpoint management, test orchestration, and data persistence
- **API Testing Engine**: Executes HTTP requests with high concurrency and retry logic
- **Frontend**: React application for creating endpoints, viewing test results, and analytics
- **Nginx**: Reverse proxy providing unified entry point, rate limiting, and request routing

For implementation details on specific components, refer to the relevant sections in the codebase or documentation files.
