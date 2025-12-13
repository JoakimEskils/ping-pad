# PingPad

A SaaS tool for testing REST API endpoints and logging webhooks — built with Spring Boot (backend) and React + Vite (frontend).

## Local Development Setup

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed and running
- Git

### Clone the repository

```bash
git clone https://github.com/yourusername/ping-pad.git
cd ping-pad
```

### Run the project with Docker Compose

run tests:
```bash
cd backend && mvn test
```

Build and run locally:

```bash
docker compose up --build
```

This will build and start two containers:

- **Backend (Spring Boot)** on http://localhost:8080
- **Frontend (React + Vite served by nginx)** on http://localhost:5173
- **API Testing Engine (Go)** on http://localhost:8081

## Architecture & Data Flow

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React         │    │   Spring Boot    │    │   Go Testing    │    │   Target APIs   │
│   Frontend      │    │   Backend        │    │   Engine        │    │   (External)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │ 1. User clicks        │                       │                       │
         │    "Test API"         │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │                       │                       │
         │                       │ 2. Get endpoint       │                       │
         │                       │    from DB            │                       │
         │                       │                       │                       │
         │                       │ 3. Convert to         │                       │
         │                       │    TestRequest        │                       │
         │                       ├──────────────────────►│                       │
         │                       │                       │                       │
         │                       │                       │ 4. Execute HTTP       │
         │                       │                       │    request            │
         │                       │                       ├──────────────────────►│
         │                       │                       │                       │
         │                       │                       │ 5. Get response      │
         │                       │                       │◄──────────────────────┤
         │                       │                       │                       │
         │                       │ 6. Return TestResult  │                       │
         │                       │◄──────────────────────┤                       │
         │                       │                       │                       │
         │                       │ 7. Save to DB         │                       │
         │                       │    (PostgreSQL)       │                       │
         │                       │                       │                       │
         │ 8. Return result      │                       │                       │
         │◄──────────────────────┤                       │                       │
         │                       │                       │                       │
```

### How it works

1. **User presses "Test API"** in the React frontend
2. **Spring Boot backend** gets endpoint from db
3. **Spring Boot** converts the endpoint to a test request and sends it to Go Testing Service
4. **Go Testing Engine** executes the actual HTTP request to the target API
5. **Go Testing Engine** returns the test result to backend
6. **Spring Boot** saves the result to db
7. **Spring Boot** returns the result to frontend

The Go Testing Engine provides high-performance HTTP testing with excellent concurrency, while Spring Boot handles business logic and data persistence via Postgres DB.
