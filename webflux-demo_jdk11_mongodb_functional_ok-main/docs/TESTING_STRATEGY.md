# Estrategia de Testing - WebFlux Demo

## Visión General

Este documento describe la estrategia completa de testing para el proyecto WebFlux Demo, siguiendo principios TDD y mejores prácticas para aplicaciones reactivas.

## Filosofía de Testing

### Principios TDD
1. **Red-Green-Refactor**: Escribir tests que fallen, implementar código mínimo para pasar, luego refactorizar
2. **Test First**: Los tests guían el diseño y la implementación
3. **Baby Steps**: Pequeños incrementos con tests frecuentes
4. **Refactoring Seguro**: Los tests protegen contra regresiones

### Principios Reactivos
1. **Testing Reactivo**: Usar StepVerifier para flujos Mono/Flux
2. **Non-blocking**: Los tests no deben bloquear innecesariamente
3. **Backpressure**: Probar comportamiento bajo presión
4. **Concurrencia**: Verificar comportamiento concurrente

## Estructura de Tests

### Jerarquía de Tests

```
src/test/java/com/example/webflux/
```

#### 1. Unit Tests
- **Service Layer**: `service/*Test.java`
- **Repository Layer**: `repository/*Test.java`  
- **Controller Layer**: `controller/*Test.java`
- **Utility Classes**: `utils/*Test.java`

#### 2. Integration Tests
- **API Integration**: `integration/*Test.java`
- **Database Integration**: `integration/*Test.java`
- **Functional Routes**: `integration/FunctionalRoutesIntegrationTest.java`

#### 3. Performance Tests
- **Concurrency**: `performance/*Test.java`
- **Load Testing**: `performance/*Test.java`
- **Stress Testing**: `performance/*Test.java`

#### 4. End-to-End Tests
- **User Workflows**: `e2e/*Test.java`
- **API Contracts**: `e2e/*Test.java`

## Tipos de Tests

### 1. Unit Tests

#### Características
- **Aislados**: Sin dependencias externas
- **Rápidos**: Ejecución en milisegundos
- **Determinísticos**: Mismo resultado siempre
- **Mocking**: Usar Mockito para dependencias

#### Ejemplo - Service Layer
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductoRepository repository;
    
    @InjectMocks
    private ProductServiceImpl service;
    
    @Test
    void findById_whenProductExists_returnsProduct() {
        // Given
        when(repository.findById("id")).thenReturn(Mono.just(product));
        
        // When
        Mono<ProductoResponse> result = service.findById("id");
        
        // Then
        StepVerifier.create(result)
            .expectNextMatches(p -> p.getId().equals("id"))
            .verifyComplete();
    }
}
```

#### Coverage Goals
- **Service Layer**: >90%
- **Repository Layer**: >80%
- **Controller Layer**: >85%
- **Utility Classes**: >95%

### 2. Integration Tests

#### Características
- **Base de Datos Real**: Testcontainers con MongoDB
- **Configuración Completa**: Spring Boot completo
- **Endpoints Reales**: WebTestClient para HTTP
- **Estado Compartido**: Base de datos entre tests

#### Ejemplo - API Integration
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class ProductoControllerIntegrationTest {
    
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");
    
    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }
    
    @Test
    void createProduct_createsSuccessfully() {
        webTestClient.post()
            .uri("/api/v2/productos")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated();
    }
}
```

### 3. Performance Tests

#### Características
- **Concurrencia**: Múltiples requests simultáneos
- **Carga**: Alto volumen de operaciones
- **Stress**: Condiciones límite
- **Monitoreo**: Métricas de rendimiento

#### Ejemplo - Concurrency Test
```java
@Test
void concurrentRequests_shouldHandleGracefully() {
    int concurrentRequests = 100;
    
    List<Mono<Void>> requests = IntStream.range(0, concurrentRequests)
        .mapToObj(i -> createRequest())
        .collect(Collectors.toList());
    
    StepVerifier.create(Flux.merge(requests))
        .expectNextCount(concurrentRequests)
        .verifyComplete();
}
```

## Estrategia por Capa

### 1. Repository Layer

#### Focus
- **Query Operations**: CRUD básico
- **Custom Queries**: Búsquedas complejas
- **Reactive Behavior**: Non-blocking operations
- **Error Handling**: Conexiones fallidas

#### Tests Clave
```java
@Test
void saveAndFind_persistsCorrectly() {
    StepVerifier.create(repository.save(product))
        .assertNext(saved -> saved.getId() != null)
        .verifyComplete();
}

@Test
void findByNombreIgnoreCase_ignoresCase() {
    StepVerifier.create(repository.findByNombreIgnoreCase("LAPTOP"))
        .expectNextMatches(p -> p.getNombre().equals("laptop"))
        .verifyComplete();
}
```

### 2. Service Layer

#### Focus
- **Business Logic**: Reglas de negocio
- **Validation**: Validaciones complejas
- **Error Handling**: Excepciones tipadas
- **Reactive Chains**: Composición de operaciones

#### Tests Clave
```java
@Test
void create_whenNameExists_throwsConflict() {
    when(repository.findByNombreIgnoreCase("name"))
        .thenReturn(Mono.just(existingProduct));
    
    StepVerifier.create(service.create(request))
        .expectError(BusinessException.Conflict.class)
        .verify();
}

@Test
void reducirStock_whenInsufficient_throwsBadRequest() {
    when(repository.findById("id")).thenReturn(Mono.just(product));
    
    StepVerifier.create(service.reducirStock("id", 100))
        .expectError(BusinessException.BadRequest.class)
        .verify();
}
```

### 3. Controller Layer

#### Focus
- **HTTP Contracts**: Request/Response
- **Validation**: Input validation
- **Error Responses**: HTTP status codes
- **Headers**: Correlation ID, CORS

#### Tests Clave
```java
@Test
void create_withValidRequest_returns201() {
    webTestClient.post()
        .uri("/api/v2/productos")
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated()
        .expectHeader().exists("X-Correlation-Id");
}

@Test
void create_withInvalidRequest_returns400() {
    webTestClient.post()
        .uri("/api/v2/productos")
        .bodyValue(invalidRequest)
        .exchange()
        .expectStatus().isBadRequest();
}
```

## Testing Reactivo

### StepVerifier Patterns

#### 1. Basic Verification
```java
StepVerifier.create(mono)
    .expectNext(expectedValue)
    .verifyComplete();
```

#### 2. Error Verification
```java
StepVerifier.create(mono)
    .expectErrorMatches(ex -> 
        ex instanceof BusinessException &&
        ex.getMessage().contains("expected message"))
    .verify();
```

#### 3. Multiple Elements
```java
StepVerifier.create(flux)
    .expectNextCount(3)
    .verifyComplete();
```

#### 4. Time-based Verification
```java
StepVerifier.create(flux)
    .expectNextWithin(Duration.ofSeconds(1), firstValue)
    .expectNextCount(2)
    .verifyComplete();
```

#### 5. Backpressure Testing
```java
StepVerifier.create(flux.onBackpressureBuffer(10))
    .expectNextCount(10)
    .verifyError();
```

### Reactive Test Patterns

#### 1. Repository Testing
```java
@Test
void reactiveRepository_handlesFluxOperations() {
    Flux<Producto> products = Flux.just(product1, product2);
    
    StepVerifier.create(repository.saveAll(products))
        .expectNextCount(2)
        .verifyComplete();
}
```

#### 2. Service Composition
```java
@Test
void serviceComposition_maintainsReactiveChain() {
    when(repository.findById("id")).thenReturn(Mono.just(product));
    when(repository.save(any())).thenReturn(Mono.just(updatedProduct));
    
    StepVerifier.create(service.update("id", request))
        .assertNext(response -> response.getNombre().equals("Updated"))
        .verifyComplete();
}
```

## Test Data Management

### Test Fixtures

#### 1. Builder Patterns
```java
private Producto createTestProduct(String name) {
    return Producto.builder()
        .nombre(name)
        .descripcion("Test: " + name)
        .precio(new BigDecimal("99.99"))
        .stock(10)
        .activo(true)
        .build();
}
```

#### 2. Test Data Factories
```java
public class TestDataFactory {
    public static ProductoRequest validRequest() {
        return ProductoRequest.builder()
            .nombre("Valid Product")
            .precio(new BigDecimal("99.99"))
            .stock(10)
            .build();
    }
    
    public static ProductoRequest invalidRequest() {
        return ProductoRequest.builder()
            .nombre("")
            .precio(new BigDecimal("-1"))
            .stock(-1)
            .build();
    }
}
```

#### 3. Database Cleanup
```java
@BeforeEach
void setUp() {
    repository.deleteAll()
        .block(Duration.ofSeconds(5));
}
```

## Error Testing

### 1. Business Exceptions
```java
@Test
void businessLogic_whenInvalid_throwsTypedException() {
    StepVerifier.create(service.invalidOperation())
        .expectError(BusinessException.Validation.class)
        .verify();
}
```

### 2. System Exceptions
```java
@Test
void systemError_whenDatabaseFails_propagatesError() {
    when(repository.findById(any()))
        .thenReturn(Mono.error(new RuntimeException("DB Error")));
    
    StepVerifier.create(service.findById("id"))
        .expectError(RuntimeException.class)
        .verify();
}
```

### 3. HTTP Error Responses
```java
@Test
void endpoint_whenError_returnsProperStatus() {
    webTestClient.get()
        .uri("/api/v2/productos/invalid")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.message").exists();
}
```

## Performance Testing

### 1. Concurrency Tests
```java
@Test
void concurrentOperations_handleLoad() {
    int concurrency = 100;
    
    StepVerifier.create(Flux.range(0, concurrency)
        .flatMap(i -> performOperation())
        .timeout(Duration.ofSeconds(30)))
        .expectNextCount(concurrency)
        .verifyComplete();
}
```

### 2. Load Testing
```java
@Test
void sustainedLoad_maintainsPerformance() {
    Duration testDuration = Duration.ofMinutes(2);
    int requestsPerSecond = 50;
    
    Flux.interval(Duration.ofMillis(1000 / requestsPerSecond))
        .take(testDuration)
        .flatMap(i -> performOperation())
        .blockLast();
}
```

### 3. Stress Testing
```java
@Test
void stressTest_handlesExtremeLoad() {
    int extremeConcurrency = 1000;
    
    StepVerifier.create(Flux.range(0, extremeConcurrency)
        .flatMap(i -> performOperation(), 100) // Limited concurrency
        .take(500)) // Limit total
        .expectNextCount(500)
        .verifyComplete();
}
```

## Test Configuration

### 1. Test Profiles
```yaml
# application-test.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/testdb
  jackson:
    default-property-inclusion: non_null

logging:
  level:
    com.example.webflux: DEBUG
```

### 2. Testcontainers Configuration
```java
@Testcontainers
class IntegrationTest {
    
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6")
        .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }
}
```

### 3. Maven Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <groups>unit</groups>
        <excludedGroups>integration,performance</excludedGroups>
    </configuration>
</plugin>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <groups>integration</groups>
    </configuration>
</plugin>
```

## Continuous Integration

### 1. Test Pipeline
```yaml
# GitHub Actions
name: Test Pipeline
on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run Unit Tests
        run: mvn test -Dgroups=unit
  
  integration-tests:
    runs-on: ubuntu-latest
    services:
      mongodb:
        image: mongo:6
    steps:
      - uses: actions/checkout@v2
      - name: Run Integration Tests
        run: mvn test -Dgroups=integration
  
  performance-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run Performance Tests
        run: mvn test -Dgroups=performance
```

### 2. Coverage Reports
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Best Practices

### 1. Test Naming
```java
// Good: Descriptive and clear
void findById_whenProductExists_returnsProductResponse()

// Bad: Vague
void testFindById()
```

### 2. Test Structure
```java
@Test
void methodName_whenCondition_expectedResult() {
    // Given - Arrange test data
    when(repository.findById("id")).thenReturn(Mono.just(product));
    
    // When - Execute operation
    Mono<ProductoResponse> result = service.findById("id");
    
    // Then - Verify results
    StepVerifier.create(result)
        .expectNextMatches(p -> p.getId().equals("id"))
        .verifyComplete();
}
```

### 3. Test Independence
```java
@BeforeEach
void setUp() {
    // Clean state before each test
    repository.deleteAll().block();
}
```

### 4. Assertion Clarity
```java
// Good: Specific assertions
assertThat(response.getNombre()).isEqualTo("Expected Name");
assertThat(response.getPrecio()).isEqualTo(new BigDecimal("99.99"));

// Bad: Generic assertions
assertThat(response).isNotNull();
```

## Métricas y Monitoreo

### 1. Coverage Metrics
- **Line Coverage**: Líneas de código ejecutadas
- **Branch Coverage**: Ramas condicionales cubiertas
- **Method Coverage**: Métodos testeados

### 2. Performance Metrics
- **Response Time**: Tiempo de respuesta promedio
- **Throughput**: Requests por segundo
- **Error Rate**: Tasa de errores

### 3. Quality Metrics
- **Test Count**: Número total de tests
- **Test Duration**: Tiempo de ejecución
- **Flaky Tests**: Tests inconsistentes

## Troubleshooting

### 1. Common Issues

#### Testcontainers Timeout
```java
@Container
static MongoDBContainer mongo = new MongoDBContainer("mongo:6")
    .withStartupTimeout(Duration.ofMinutes(2));
```

#### Reactive Blocking
```java
// Avoid this
Mono<Producto> result = service.findById("id").block();

// Use this instead
StepVerifier.create(service.findById("id"))
    .expectNextCount(1)
    .verifyComplete();
```

#### Mock Verification
```java
// Verify interactions
verify(repository).findById("id");
verify(repository, never()).delete(any());
```

### 2. Debugging Tips

#### Enable Debug Logging
```yaml
logging:
  level:
    reactor.core: DEBUG
    org.springframework.data.mongodb: DEBUG
```

#### Test Isolation
```java
@TestMethodOrder(OrderAnnotation.class)
class OrderedTest {
    @Test
    @Order(1)
    void firstTest() { }
}
```

Esta estrategia de testing proporciona una base sólida para mantener la calidad y confiabilidad del proyecto WebFlux Demo.
