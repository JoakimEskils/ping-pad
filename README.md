# PingPad

A SaaS tool for testing REST API endpoints and logging webhooks — built with Spring Boot (backend) and React + Vite (frontend).

---

## Local Development Setup

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed and running
- Git

---

### Clone the repository

```bash
git clone https://github.com/yourusername/ping-pad.git
cd ping-pad

### Run the project with Docker Compose

```bash
docker compose up --build

This will build and start two containers:

- Backend (Spring Boot) on http://localhost:8080
- Frontend (React + Vite served by nginx) on http://localhost:5173
