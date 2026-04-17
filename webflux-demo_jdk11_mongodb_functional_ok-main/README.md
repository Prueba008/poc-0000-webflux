# Spring WebFlux Demo (JDK 11) + MongoDB Reactive

Proyecto funcional de Spring Boot 2.7.x + WebFlux + MongoDB Reactivo, con:
- **Arquitectura Reactiva**: WebFlux + MongoDB Reactive Streams
- **Exceptions tipadas** y manejo global de errores (JSON uniforme)
- **OpenAPI/Swagger UI** (springdoc webflux)
- **Validación consistente** en controllers y functional routes
- **Observabilidad**: Correlation-ID + logs JSON + Actuator/Prometheus + tags custom
- **Tests con Testcontainers** (MongoDB) y WebTestClient
- **Operaciones masivas** optimizadas con concurrencia controlada
- **Streaming en tiempo real** con Server-Sent Events (SSE)

## Documentación Completa

- **[Arquitectura](docs/ARCHITECTURE.md)** - Detalles técnicos, patrones y diseño del sistema
- **[API Reference](docs/API_REFERENCE.md)** - Documentación completa de todos los endpoints
- **[Guía de Desarrollo](docs/DEVELOPMENT_GUIDE.md)** - Guía completa para desarrolladores
- **[Estrategia de Testing](docs/TESTING_STRATEGY.md)** - Estrategia completa de testing TDD
- **[Resumen de Bug Fixes](docs/BUG_FIXES_FINAL.md)** - Documentación completa de bugs corregidos

## Estado Actual del Proyecto

### ✅ Bugs Críticos Corregidos
1. **Service Interface Mismatch** - Corregida firma incorrecta en `ProductService`
2. **Bulk Controller Logic Error** - Corregido flujo de datos en operaciones masivas  
3. **Controller Error Handling** - Mejorado manejo de errores reactivos
4. **Type Inference Issues** - Resueltos problemas de inferencia de tipos
5. **Ambiguous Mapping** - Eliminados conflictos de endpoints duplicados

### 🚀 Mejoras Implementadas
- **Type Safety**: Mejorada seguridad de tipos en DTOs y controladores
- **Error Handling**: Manejo comprensivo de errores con códigos HTTP correctos
- **Reactive Patterns**: Aplicación correcta de patrones reactivos
- **Performance**: Optimizaciones en operaciones bulk y streaming
- **Testing**: Cobertura completa con tests unitarios, de integración y de rendimiento

## Requisitos
- **JDK 11** (versión mínima requerida)
- **Maven 3.6+** (para construcción y gestión de dependencias)
- **Docker** (opcional, para MongoDB y stack completo de monitoreo)
- **MongoDB 6+** (si se ejecuta sin Docker)

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

## Endpoints Principales

### API v1 (Anotada) - Gestión Individual
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/productos/activos` | Listar productos activos |
| GET | `/api/v1/productos/{id}` | Obtener producto por ID |
| GET | `/api/v1/productos/buscar` | Búsqueda polimórfica (nombre/categoría/precio) |
| POST | `/api/v1/productos` | Crear nuevo producto |
| PUT | `/api/v1/productos/{id}` | Actualizar producto completo |
| POST | `/api/v1/productos/{id}/reducir-stock` | Reducción de stock con validación |
| GET | `/api/v1/productos/stream` | Streaming real-time (SSE) |

### API v2 (Functional Routes) - CRUD Básico
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v2/productos` | Listar todos los productos |
| GET | `/api/v2/productos/{id}` | Obtener producto por ID |
| POST | `/api/v2/productos` | Crear nuevo producto |
| PUT | `/api/v2/productos/{id}` | Actualizar producto |
| DELETE | `/api/v2/productos/{id}` | Eliminar producto |
| GET | `/api/v2/productos/stream` | Streaming real-time (SSE) |

### API v2 (Bulk Operations) - Operaciones Masivas
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| PUT | `/api/v2/productos/bulk/update` | Actualización masiva con concurrencia controlada |

## Características Técnicas

### Correlation ID
La aplicación usa el header `X-Correlation-Id` para trazabilidad distribuida:
- **Si el cliente lo envía**: Se respeta el valor proporcionado
- **Si no**: Se genera un UUID único y se devuelve en la respuesta
- **Propagación**: El valor se propaga en logs (MDC) y Reactor Context

### Manejo de Errores
- **Excepciones tipadas**: `ProductoNotFoundException`, `ValidationException`
- **Respuesta uniforme**: JSON con estructura consistente `{timestamp, status, error, message, path, correlationId}`
- **Global Error Handler**: Centralizado para todas las excepciones

### Observabilidad
- **Logs JSON**: Formato estructurado con logstash-logback-encoder
- **Métricas Prometheus**: Custom tags para aplicación y entorno
- **Health Checks**: Endpoint `/actuator/health` con indicadores
- **Correlation tracing**: Propagación automática en toda la cadena

## Configuración

### Perfiles de Ejecución
- **dev**: Desarrollo local (default)
- **prod**: Producción con optimizaciones
- **test**: Tests automatizados
- **tc**: Tests con Testcontainers

### Variables de Entorno Clave
```bash
# Base de datos
MONGODB_URI=mongodb://localhost:27017/webfluxdb

# CORS
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://frontend.example.com

# Correlation ID
APP_CORRELATION_HEADER=X-Correlation-Id

# Métricas
SPRING_APPLICATION_NAME=webflux-demo
SPRING_PROFILES_ACTIVE=prod
```

## Tests

### Ejecución de Tests
```bash
# Tests unitarios y de integración
mvn test

# Tests con Testcontainers (requiere Docker)
mvn test -P tc

# Reporte de cobertura
target/site/jacoco/index.html
```

### Estrategia de Testing
- **Unit Tests**: Lógica de negocio con Mockito
- **Integration Tests**: WebTestClient para endpoints
- **Testcontainers**: MongoDB real en contenedor aislado
- **Reactive Testing**: Reactor Test para flujos reactivos
- **Coverage**: JaCoCo con reportes HTML

## Monitoreo y Observabilidad

### Stack de Monitoreo (Docker Compose)
- **Prometheus**: Recopilación de métricas (http://localhost:9090)
- **Grafana**: Dashboards y visualizaciones (http://localhost:3000, admin/admin)
- **Actuator**: Health, metrics, info endpoints

### Métricas Disponibles
- **HTTP Metrics**: Requests, duration, status codes
- **JVM Metrics**: Memory, GC, threads
- **Custom Metrics**: Business operations, bulk operations
- **Reactive Metrics**: Backpressure, subscription events

## Rendimiento y Optimización

### Concurrencia Controlada
- **Bulk Operations**: Límite de 32 operaciones concurrentes
- **Backpressure**: Manejo automático de presión en flujos reactivos
- **Connection Pooling**: Optimizado para MongoDB reactive driver

### Streaming
- **Server-Sent Events**: Streaming real-time de productos
- **Delay Elements**: Emisión cada 2 segundos para demostración
- **Non-blocking**: Sin bloqueo de hilos durante streaming
