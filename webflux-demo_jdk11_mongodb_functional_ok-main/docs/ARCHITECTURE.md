# Arquitectura del Proyecto - WebFlux Demo

## Visión General

Este proyecto implementa una API REST reactiva utilizando Spring Boot 2.7.x + WebFlux + MongoDB Reactive, demostrando mejores prácticas en programación reactiva, manejo de errores, observabilidad y testing.

## Stack Tecnológico

### Core Framework
- **Spring Boot 2.7.18**: Framework base con soporte reactivo
- **Spring WebFlux**: Stack web no bloqueante basado en Netty
- **Project Reactor**: Librería reactiva (Mono/Flux)

### Base de Datos
- **MongoDB 6+**: Base de datos NoSQL documental
- **Spring Data MongoDB Reactive**: Driver reactivo para MongoDB
- **Reactive Streams**: API estándar para programación reactiva

### Documentación y Validación
- **SpringDoc OpenAPI 1.7.0**: Documentación automática de API
- **Bean Validation**: Validación con anotaciones estándar
- **Swagger UI**: Interfaz interactiva de documentación

### Observabilidad y Monitoreo
- **Micrometer**: Facade para métricas
- **Prometheus**: Sistema de monitoreo de métricas
- **Grafana**: Visualización de dashboards
- **Actuator**: Endpoints de monitoreo de Spring Boot
- **Logstash Logback Encoder**: Logs en formato JSON estructurado

### Testing
- **Testcontainers 1.19.3**: Contenedores para tests de integración
- **WebTestClient**: Cliente HTTP reactivo para tests
- **Reactor Test**: Utilidades para testing reactivo
- **JaCoCo**: Cobertura de código

## Estructura del Proyecto

```
src/main/java/com/example/webflux/
```

### Paquetes Principales

#### `config/`
Configuración centralizada de la aplicación:
- `WebFluxConfig`: CORS, WebClient, métricas
- `DataInitializer`: Carga inicial de datos

#### `controller/`
Endpoints REST anotados:
- `ProductoController`: API v1 anotada con operaciones individuales
- `ProductoBulkControllerV2`: API v2 para operaciones masivas

#### `router/` & `handler/`
Enfoque funcional para rutas:
- `ProductoRouter`: Definición de rutas funcionales (API v2)
- `ProductoHandler`: Lógica de negocio para rutas funcionales

#### `service/`
Lógica de negocio reactiva:
- `ProductService`: Operaciones CRUD y búsquedas avanzadas
- Servicios especializados para diferentes dominios

#### `repository/`
Acceso a datos reactivo:
- `ProductoRepository`: Reactive MongoDB Repository

#### `model/`
Entidades y DTOs:
- `Producto`: Entidad MongoDB
- DTOs para requests/responses
- Modelos para operaciones bulk

#### `exception/`
Manejo de errores:
- Excepciones tipadas personalizadas
- `GlobalErrorWebExceptionHandler`: Manejo centralizado

#### `observability/`
Trazabilidad y logging:
- `CorrelationFilter`: Filtro para correlation ID
- `LoggingConfig`: Configuración de logs estructurados

#### `validation/`
Soporte de validación:
- `ValidationSupport`: Utilidades de validación consistentes

## Patrones Arquitectónicos

### 1. Programación Reactiva
- **Non-blocking I/O**: Todas las operaciones son no bloqueantes
- **Backpressure**: Manejo automático de presión en flujos de datos
- **Reactive Streams**: Uso de Mono (0-1 elementos) y Flux (0-N elementos)

### 2. Dual Approach: Anotado vs Funcional
- **API v1 (Anotada)**: @RestController con anotaciones Spring
- **API v2 (Funcional)**: RouterFunction + HandlerFunction
- Ambos enfoques coexisten para demostrar diferentes estilos

### 3. Manejo de Errores Centralizado
- **Excepciones Tipadas**: Clases específicas para cada tipo de error
- **Global Handler**: Interceptor centralizado para todas las excepciones
- **Respuesta Uniforme**: Estructura JSON consistente para todos los errores

### 4. Observabilidad End-to-End
- **Correlation ID**: Trazabilidad distribuida a través de todos los componentes
- **Logs Estructurados**: JSON con campos consistentes para análisis
- **Métricas Custom**: Indicadores de negocio además de métricas técnicas

### 5. Testing Estratificado
- **Unit Tests**: Lógica de negocio aislada
- **Integration Tests**: Endpoints con WebTestClient
- **Container Tests**: MongoDB real con Testcontainers

## Flujo de Datos Típico

### Request Lifecycle
1. **Correlation Filter**: Asigna o extrae correlation ID
2. **CORS Filter**: Aplica políticas de origen cruzado
3. **Router/Controller**: Enruta request al handler apropiado
4. **Service Layer**: Ejecuta lógica de negocio reactiva
5. **Repository**: Acceso a datos MongoDB reactivo
6. **Response**: Transformación a DTO y retorno reactivo

### Error Handling Flow
1. **Exception Occurs**: En cualquier capa de la aplicación
2. **Global Handler**: Captura excepción
3. **Error Mapping**: Convierte a respuesta estandarizada
4. **Logging**: Registra error con correlation ID
5. **Response**: Retorna JSON con estructura consistente

## Configuración y Perfiles

### Perfiles de Ejecución
- **dev**: Desarrollo local con configuraciones por defecto
- **prod**: Producción con optimizaciones y seguridad
- **test**: Tests automatizados
- **tc**: Tests con Testcontainers

### Configuración Clave
- **MongoDB URI**: Configurable por entorno
- **CORS Orígenes**: Lista de orígenes permitidos
- **Métricas**: Tags comunes para aplicación y entorno
- **Logging**: Formato JSON con correlation ID

## Consideraciones de Rendimiento

### Concurrencia
- **Event Loop**: Netty event loop para I/O no bloqueante
- **Connection Pooling**: Optimizado para MongoDB reactive driver
- **Bulk Operations**: Concurrencia controlada (límite de 32 operaciones)

### Memoria
- **Heap Management**: Configuración JVM para contenedores
- **Backpressure**: Prevención de overflow en flujos reactivos
- **Lazy Evaluation**: Procesamiento bajo demanda

### Escalabilidad
- **Horizontal Scaling**: Soporte para múltiples instancias
- **Stateless Design**: Sin estado compartido entre requests
- **Reactive MongoDB**: Driver optimizado para alta concurrencia

## Seguridad

### CORS
- Configuración reactiva de CORS
- Orígenes permitidos configurables
- Headers expuestos para correlation ID

### Validación
- Bean Validation estándar
- Validación consistente en ambos enfoques
- Soporte para validaciones complejas

### Observabilidad
- Trazabilidad completa de requests
- Logs estructurados para análisis forense
- Métricas de seguridad y errores

Esta arquitectura proporciona una base sólida para aplicaciones reactivas enterprise-ready con observabilidad completa y testing exhaustivo.
