# Use official OpenJDK image as build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom and source code
COPY backend/pom.xml .
COPY backend/src ./src

# Copy proto files from api-testing-engine (needed for code generation)
# The pom.xml expects proto files at ../api-testing-engine/proto relative to /app
# So we need to create /api-testing-engine/proto (one level up from /app)
RUN mkdir -p /api-testing-engine
COPY api-testing-engine/proto /api-testing-engine/proto

# Build the app (package jar)
RUN mvn clean package -DskipTests

# Use a smaller JRE image to run the app
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port your Spring Boot app uses
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
