# Spring WebFlux Demo (JDK 11) + MongoDB Reactive

Proyecto funcional de Spring Boot 2.7.x + WebFlux + MongoDB Reactivo, con:
- Exceptions tipadas y manejo global de errores (JSON uniforme)
- OpenAPI/Swagger UI (springdoc webflux)
- Validación consistente en controllers y functional routes
- Observabilidad: Correlation-ID + logs JSON + Actuator/Prometheus + tags custom
- Tests con Testcontainers (MongoDB) y WebTestClient

## Requisitos
- JDK 11
- Maven 3.6+
- Docker (opcional)

## Ejecutar en local (dev)
1) Levantar MongoDB local:
```bash
docker run --rm -p 27017:27017 --name mongo mongo:6
```

2) Ejecutar app:
```bash
mvn spring-boot:run
```

## Docker Compose (app + mongo + prometheus + grafana)
```bash
docker-compose up -d --build
```

- App: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator health: http://localhost:8080/actuator/health
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

## Endpoints principales
### API v1 (anotada)
- GET    /api/v1/productos
- GET    /api/v1/productos/{id}
- POST   /api/v1/productos
- PUT    /api/v1/productos/{id}
- PATCH  /api/v1/productos/{id}
- DELETE /api/v1/productos/{id}
- GET    /api/v1/productos/stream

### API v2 (functional routes)
- GET    /api/v2/productos
- GET    /api/v2/productos/{id}
- POST   /api/v2/productos
- PUT    /api/v2/productos/{id}
- DELETE /api/v2/productos/{id}
- GET    /api/v2/productos/stream

## Correlation ID
La app usa el header `X-Correlation-Id`.
- Si el cliente lo envía, se respeta.
- Si no, se genera uno y se devuelve en la respuesta.
Este valor se propaga en logs (MDC) y Reactor Context.

## Tests
```bash
mvn test
```
Incluye Testcontainers de MongoDB.
