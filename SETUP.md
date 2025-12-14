# Setup Guide

This guide explains how to set up the PingPad project for local development and production deployment.

## Local Development Setup

### Option 1: Docker Compose (Recommended)

The easiest way to run the project is using Docker Compose, which handles all services automatically.

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ping-pad.git
   cd ping-pad
   ```

2. **Start all services**
   ```bash
   docker compose up --build
   ```

3. **Access the application**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080
   - API Testing Engine: http://localhost:8081

**That's it!** Docker Compose handles all environment variables automatically.

### Option 2: Manual Setup with .env File

If you want to customize configuration or run services separately:

1. **Create `.env` file in project root**
   ```bash
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=ping-pad-db
   DB_USER=postgres
   DB_PASSWORD=your_password
   REDIS_HOST=localhost
   REDIS_PORT=6379
   JWT_SECRET=your-256-bit-secret-key-must-be-at-least-32-characters-long
   API_TESTING_ENGINE_URL=http://localhost:8081
   ```

2. **Create `frontend/.env` file** (optional)
   ```bash
   VITE_BACKEND_URL=http://localhost:8080
   ```

3. **Start services manually**
   - Start PostgreSQL and Redis
   - Start API Testing Engine
   - Start Backend
   - Start Frontend

## GitHub Secrets Setup (for CI/CD)

If you're contributing or setting up CI/CD, you need to configure GitHub Secrets.

### Where to Add Secrets

1. Go to your GitHub repository
2. Navigate to: **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

### Required Secrets

Add these secrets for CI/CD to work:

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `POSTGRES_USER` | PostgreSQL username | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `your_secure_password` |
| `POSTGRES_DB` | PostgreSQL database name | `pingpad` |

### How Secrets Are Used

Secrets are automatically injected into GitHub Actions workflows. You don't need to fetch them manually - they're available as `${{ secrets.SECRET_NAME }}` in workflow files.

**Example in workflow:**
```yaml
env:
  POSTGRES_USER: ${{ secrets.POSTGRES_USER }}
  POSTGRES_PASSWORD: ${{ secrets.POSTGRES_PASSWORD }}
  POSTGRES_DB: ${{ secrets.POSTGRES_DB }}
```

### For Contributors

If you're cloning the repository to contribute:

1. **You don't need GitHub Secrets for local development** - use `.env` files or Docker Compose defaults
2. **GitHub Secrets are only for CI/CD** - they're used when GitHub Actions runs tests
3. **If CI fails due to missing secrets**, ask the repository maintainer to add them

## Environment Variables Reference

See [ENV_VARIABLES.md](ENV_VARIABLES.md) for a complete list of all environment variables.

## Troubleshooting

### Port Already in Use

If you get port conflicts:
```bash
# Check what's using the port
lsof -i :8080
lsof -i :5173
lsof -i :5432

# Stop conflicting services or change ports in docker-compose.yml
```

### Database Connection Issues

If the backend can't connect to PostgreSQL:
1. Check that PostgreSQL container is running: `docker compose ps`
2. Verify environment variables match in `docker-compose.yml`
3. Check logs: `docker compose logs postgres`

### Frontend Can't Connect to Backend

1. Verify `VITE_BACKEND_URL` is set correctly
2. Check backend is running: `docker compose logs backend`
3. Verify CORS settings in backend configuration
