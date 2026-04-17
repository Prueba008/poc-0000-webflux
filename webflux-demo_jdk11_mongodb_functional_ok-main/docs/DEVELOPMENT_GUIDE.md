# Guía de Desarrollo - WebFlux Demo

## Configuración del Entorno de Desarrollo

### Prerrequisitos
- JDK 11+
- Maven 3.6+
- Docker (opcional, para MongoDB y Testcontainers)
- IDE con soporte para Spring Boot (IntelliJ IDEA recomendado)

### Configuración del IDE

#### IntelliJ IDEA
1. Instalar plugins:
   - Spring Boot Helper
   - Lombok Plugin
   - Docker Plugin

2. Configurar JDK 11:
   ```
   File > Project Structure > Project SDK > 11
   ```

3. Habilitar annotation processing:
   ```
   File > Settings > Build > Compiler > Annotation Processors
   Enable annotation processing
   ```

#### VS Code
1. Instalar extensiones:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok and Annotations

2. Configurar settings.json:
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.compile.nullAnalysis.mode": "automatic"
   }
   ```

## Ejecución y Debug

### Modo Desarrollo
```bash
# Ejecución normal
mvn spring-boot:run

# Con perfil específico
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Con JVM options
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m"
```

### Debug Remoto
```bash
# Ejecución con debug port habilitado
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Docker Compose (Stack Completo)
```bash
# Iniciar todos los servicios
docker-compose up -d --build

# Ver logs de la aplicación
docker-compose logs -f webflux-app

# Detener servicios
docker-compose down
```

## Estructura del Proyecto

### Paquetes y Responsabilidades

```
com.example.webflux/
```

#### `config/`
- **WebFluxConfig**: Configuración CORS, WebClient, métricas
- **DataInitializer**: Carga inicial de datos para desarrollo

#### `controller/`
- **ProductoController**: API v1 anotada (gestión individual)
- **ProductoBulkControllerV2**: API v2 bulk operations

#### `router/` & `handler/`
- **ProductoRouter**: Rutas funcionales (API v2)
- **ProductoHandler**: Lógica para rutas funcionales

#### `service/`
- **ProductService**: Lógica de negocio principal
- Otros servicios de dominio

#### `repository/`
- **ProductoRepository**: Reactive MongoDB Repository

#### `model/`
- **Producto**: Entidad MongoDB
- **dto/**: Data Transfer Objects
- **bulk/**: Modelos para operaciones masivas

#### `exception/`
- **GlobalErrorWebExceptionHandler**: Manejo centralizado de errores
- Excepciones tipadas personalizadas

#### `observability/`
- **CorrelationFilter**: Filtro para trazabilidad
- **LoggingConfig**: Configuración de logs

## Patrones y Buenas Prácticas

### Programación Reactiva

#### Uso de Mono vs Flux
```java
// Mono: 0-1 elementos
public Mono<Producto> findById(String id) {
    return repository.findById(id);
}

// Flux: 0-N elementos  
public Flux<Producto> findAll() {
    return repository.findAll();
}
```

#### Operaciones Comunes
```java
// Transformación
public Flux<ProductoResponse> toResponse(Flux<Producto> productos) {
    return productos.map(this::convertToResponse);
}

// Filtrado
public Flux<Producto> findActivos() {
    return repository.findAll()
        .filter(Producto::isActivo);
}

// Manejo de errores
public Mono<Producto> findById(String id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new ProductoNotFoundException(id)));
}
```

#### Combinación de Flujos
```java
// Zip: Combinar múltiples fuentes
public Mono<DetalleCompleto> getDetalleCompleto(String id) {
    Mono<Producto> producto = repository.findById(id);
    Mono<StockInfo> stock = stockService.getStock(id);
    Mono<PrecioHistorico> precio = precioService.getHistorico(id);
    
    return Mono.zip(producto, stock, precio)
        .map(tuple -> new DetalleCompleto(tuple.getT1(), tuple.getT2(), tuple.getT3()));
}
```

### Manejo de Errores

#### Excepciones Tipadas
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductoNotFoundException extends RuntimeException {
    public ProductoNotFoundException(String id) {
        super("Producto no encontrado con ID: " + id);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
```

#### Global Error Handler
```java
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Lógica centralizada de manejo de errores
    }
}
```

### Validación

#### Bean Validation
```java
public class ProductoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
    
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;
}
```

#### Validación en Controllers
```java
@PostMapping
public Mono<ResponseEntity<ProductoResponse>> create(
        @Valid @RequestBody ProductoRequest request) {
    return productService.create(request)
        .map(ResponseEntity::ok);
}
```

#### Validación en Functional Routes
```java
public Mono<ServerResponse> create(ServerRequest request) {
    return request.bodyToMono(ProductoRequest.class)
        .flatMap(body -> {
            ValidationResult validation = validateRequest(body);
            if (!validation.isValid()) {
                return ServerResponse.badRequest()
                    .body(validation.getErrors(), ErrorDetail.class);
            }
            return productService.create(body)
                .flatMap(saved -> ServerResponse.created(URI.create("/api/v2/productos/" + saved.getId()))
                    .body(saved, ProductoResponse.class));
        });
}
```

## Testing

### Estrategia de Testing

#### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductoRepository repository;
    
    @InjectMocks
    private ProductService service;
    
    @Test
    void shouldFindById() {
        // Given
        String id = "test-id";
        Producto producto = new Producto();
        when(repository.findById(id)).thenReturn(Mono.just(producto));
        
        // When
        Mono<Producto> result = service.findById(id);
        
        // Then
        StepVerifier.create(result)
            .expectNext(producto)
            .verifyComplete();
    }
}
```

#### Integration Tests
```java
@SpringBootTest
@AutoConfigureWebTestClient
class ProductoControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldGetProductoById() {
        webTestClient.get()
            .uri("/api/v1/productos/{id}", "test-id")
            .exchange()
            .expectStatus().isOk()
            .expectBody(ProductoResponse.class);
    }
}
```

#### Testcontainers
```java
@Testcontainers
class ProductoRepositoryTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Test
    void shouldSaveAndFindProducto() {
        // Test con MongoDB real en contenedor
    }
}
```

### Reactive Testing
```java
@Test
void shouldHandleBackpressure() {
    Flux<Integer> numbers = Flux.range(1, 1000)
        .onBackpressureBuffer(10); // Buffer limitado
    
    StepVerifier.create(numbers.take(5))
        .expectNext(1, 2, 3, 4, 5)
        .verifyComplete();
}
```

## Debugging y Troubleshooting

### Logs Estructurados
```json
{
  "@timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.example.webflux.service.ProductService",
  "message": "Producto creado exitosamente",
  "correlation_id": "550e8400-e29b-41d4-a716-446655440000",
  "application": "webflux-demo",
  "env": "dev"
}
```

### Métricas Útiles
- `http.server.requests`: Requests HTTP
- `mongodb.driver.commands`: Operaciones MongoDB
- `reactor.flow.active`: Flujos reactivos activos
- `jvm.memory.used`: Memoria utilizada

### Problemas Comunes

#### Blocking Operations
```java
// INCORRECTO - Operación bloqueante en contexto reactivo
public Mono<String> processData() {
    String result = blockingService.process(); // Bloquea el thread
    return Mono.just(result);
}

// CORRECTO - Operación no bloqueante
public Mono<String> processData() {
    return Mono.fromCallable(() -> blockingService.process())
        .subscribeOn(Schedulers.boundedElastic());
}
```

#### Thread Pool Exhaustion
```java
// Configurar schedulers apropiados
@Bean
public Scheduler boundedElasticScheduler() {
    return Schedulers.newBoundedElastic(
        10,  // max threads
        100, // task queue capacity
        "bounded-elastic"
    );
}
```

## Performance y Optimización

### Concurrencia
```java
// Bulk operations con concurrencia controlada
public Mono<BulkOperationResult> updateBulk(List<ProductoRequest> requests) {
    final int CONCURRENCY_LIMIT = 32;
    
    return Flux.fromIterable(requests)
        .flatMap(this::updateProducto, CONCURRENCY_LIMIT)
        .collectList()
        .map(this::buildResult);
}
```

### Caching
```java
@Service
public class ProductService {
    
    @Cacheable(value = "productos", key = "#id")
    public Mono<Producto> findById(String id) {
        return repository.findById(id);
    }
    
    @CacheEvict(value = "productos", key = "#producto.id")
    public Mono<Producto> save(Producto producto) {
        return repository.save(producto);
    }
}
```

### Connection Pooling
```yaml
spring:
  data:
    mongodb:
      options:
        max-connection-per-host: 10
        max-connection-idle-time: 60000
        max-connection-life-time: 120000
```

## Despliegue

### Docker
```dockerfile
FROM openjdk:11-jre-slim

COPY target/webflux-demo-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webflux-demo
spec:
  replicas: 3
  selector:
    matchLabels:
      app: webflux-demo
  template:
    metadata:
      labels:
        app: webflux-demo
    spec:
      containers:
      - name: webflux-demo
        image: webflux-demo:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: mongodb-uri
```

## Monitoreo en Producción

### Endpoints de Actuator
- `/actuator/health` - Estado de la aplicación
- `/actuator/metrics` - Métricas disponibles
- `/actuator/prometheus` - Métricas para Prometheus
- `/actuator/info` - Información de la aplicación

### Dashboards de Grafana
- HTTP Request Metrics
- JVM Performance
- MongoDB Operations
- Reactive Flow Metrics

### Alertas
- Alta tasa de errores HTTP (>5%)
- Memoria JVM >80%
- Conexiones MongoDB agotadas
- Latencia de endpoints >1s

Esta guía proporciona toda la información necesaria para desarrollar, testear y desplegar aplicaciones WebFlux de manera efectiva.
