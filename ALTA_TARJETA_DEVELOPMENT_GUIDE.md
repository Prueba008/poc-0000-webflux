# Alta Tarjeta Credito - Development Guide

## Project Overview
Enterprise-ready REST API for credit card management with Java 21, Spring Boot 3.2.5, implementing Luhn validation, structured logging, H2 persistence, Micrometer metrics, and OpenAPI documentation.

## Build & Configuration Instructions

### Prerequisites
- Java 21 (JDK 21+)
- Maven 3.8+
- IDE with Spring Boot support (IntelliJ IDEA recommended)

### Build Commands
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn clean package

# Run Spring Boot application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### Configuration Files
- **`src/main/resources/application.properties`** - Main configuration
- **`src/test/resources/application-test.properties`** - Test configuration (H2 in-memory)
- **`src/main/resources/logback.xml`** - Logging configuration

### Key Configuration Properties
```properties
# Database
spring.datasource.url=jdbc:h2:./data/tarjetas;AUTO_SERVER=TRUE
spring.jpa.hibernate.ddl-auto=update

# Actuator Endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Environment-Specific Configuration
- **Development**: Uses file-based H2 database with console enabled
- **Test**: Uses in-memory H2 database, disabled Swagger UI
- **Production**: Externalize database URL and credentials via environment variables
