# Test Structure Documentation

This document describes the test structure for the PingPad project, which includes both unit tests and system tests for the Java backend and Go API testing engine.

## Test Organization

### Java Backend Tests

#### Unit Tests (`backend/src/test/java/com/pingpad/modules/api_testing/unit/`)

Unit tests focus on testing individual components in isolation with mocked dependencies:

- **`ApiEndpointControllerUnitTest.java`**: Tests the REST controller endpoints
  - Creating endpoints (success and validation failures)
  - Updating endpoints
  - Deleting endpoints
  - Getting endpoints (single and list)
  - Testing endpoints
  - Header handling

- **`ApiEndpointServiceUnitTest.java`**: Tests the service layer business logic
  - Creating endpoints with event sourcing
  - Updating endpoints
  - Deleting endpoints
  - Cache-aside pattern for reads
  - Event store interactions

- **`ApiTestServiceUnitTest.java`**: Tests the API testing service
  - Successful endpoint testing via gRPC
  - POST requests with body
  - Error handling (gRPC errors, missing endpoints/users)
  - Header processing

#### System Tests (`backend/src/test/java/com/pingpad/modules/api_testing/system/`)

System tests verify complete flows through multiple services and the database:

- **`EndpointFlowSystemTest.java`**: Tests end-to-end flows
  - Create-Read-Update-Delete (CRUD) flow
  - Create and list endpoints flow
  - Validation flow
  - Error scenarios (non-existent endpoints)

### Go API Testing Engine Tests

#### Unit Tests

- **`pkg/testing/engine_test.go`**: Tests the core testing engine
  - Engine initialization
  - Successful HTTP requests
  - Error handling (404, invalid URLs)
  - Header handling
  - POST requests with body
  - Batch test execution
  - Metrics collection

- **`internal/grpc/server_test.go`**: Tests the gRPC server
  - Single endpoint testing
  - Batch testing
  - Health checks
  - Metrics retrieval
  - Invalid timeout handling
  - POST requests with body

#### System Tests (`api-testing-engine/system/`)

- **`grpc_flow_test.go`**: Tests complete gRPC flows
  - End-to-end gRPC communication
  - POST request handling
  - Error handling scenarios
  - Health and metrics endpoints

## Running Tests

### Java Backend Tests

```bash
cd backend
./mvnw test
```

To run only unit tests:
```bash
./mvnw test -Dtest="**/unit/**"
```

To run only system tests:
```bash
./mvnw test -Dtest="**/system/**"
```

### Go Tests

```bash
cd api-testing-engine
go test ./...
```

To run only unit tests:
```bash
go test ./pkg/testing/... ./internal/grpc/...
```

To run only system tests:
```bash
go test ./system/...
```

To run with verbose output:
```bash
go test -v ./...
```

## Test Coverage

### Unit Tests Coverage

- **Controllers**: All REST endpoints with various scenarios
- **Services**: Business logic with mocked dependencies
- **Engine**: HTTP request execution, retry logic, metrics
- **gRPC Server**: Protocol buffer handling, request/response conversion

### System Tests Coverage

- **Full CRUD flows**: Creating, reading, updating, and deleting endpoints
- **Database integration**: Verifying data persistence
- **gRPC communication**: Complete request/response cycles
- **Error scenarios**: Handling of invalid inputs and missing resources

## Test Principles

1. **Unit Tests**: Fast, isolated, with all dependencies mocked
2. **System Tests**: Slower, test real integrations (database, gRPC), use `@Transactional` for cleanup
3. **Clear Separation**: Unit tests in `unit/` folder, system tests in `system/` folder
4. **Comprehensive**: Cover success paths, error paths, and edge cases
5. **Maintainable**: Well-structured, readable, and easy to extend

## Notes

- System tests use H2 in-memory database for Java backend
- System tests use real HTTP servers (httptest) for Go tests
- All tests are designed to be run independently and in parallel where possible
- Test data is cleaned up automatically (Java uses `@Transactional`, Go uses defer/cleanup)
