# API Reference - WebFlux Demo

## Visión General

Este documento describe en detalle todos los endpoints disponibles en la API WebFlux Demo, incluyendo parámetros, respuestas, ejemplos y casos de uso.

## Base URL
- **Local**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## Headers Globales

### Headers Requeridos
```
Content-Type: application/json
Accept: application/json
```

### Headers Opcionales
```
X-Correlation-Id: <UUID>  # Para trazabilidad distribuida
```

## Modelo de Datos

### Producto (Entidad)
```json
{
  "id": "507f1f77bcf86cd799439011",
  "nombre": "Laptop Gaming",
  "descripcion": "Laptop de alto rendimiento para gaming",
  "precio": 1299.99,
  "stock": 25,
  "categoria": "Electrónica",
  "activo": true,
  "fechaCreacion": "2024-01-01T12:00:00.000Z",
  "fechaActualizacion": "2024-01-01T12:00:00.000Z"
}
```

### ProductoRequest (Creación/Actualización)
```json
{
  "nombre": "Laptop Gaming",
  "descripcion": "Laptop de alto rendimiento para gaming",
  "precio": 1299.99,
  "stock": 25,
  "categoria": "Electrónica",
  "activo": true
}
```

### ProductoResponse (Respuesta API)
```json
{
  "id": "507f1f77bcf86cd799439011",
  "nombre": "Laptop Gaming",
  "descripcion": "Laptop de alto rendimiento para gaming",
  "precio": 1299.99,
  "stock": 25,
  "categoria": "Electrónica",
  "activo": true,
  "disponible": true,
  "fechaCreacion": "2024-01-01T12:00:00.000Z",
  "fechaActualizacion": "2024-01-01T12:00:00.000Z"
}
```

---

## API v1 - Gestión Individual (Anotada)

### 1. Listar Productos Activos

**GET** `/api/v1/productos/activos`

Recupera todos los productos con estado `activo = true`.

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "507f1f77bcf86cd799439011",
    "nombre": "Laptop Gaming",
    "precio": 1299.99,
    "stock": 25,
    "activo": true,
    "disponible": true,
    "categoria": "Electrónica"
  }
]
```

#### Ejemplo cURL
```bash
curl -X GET http://localhost:8080/api/v1/productos/activos
```

---

### 2. Obtener Producto por ID

**GET** `/api/v1/productos/{id}`

Recupera un producto específico por su identificador único.

#### Parámetros de Path
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| id | String | ID único del producto |

#### Response Exitoso (200 OK)
```json
{
  "id": "507f1f77bcf86cd799439011",
  "nombre": "Laptop Gaming",
  "precio": 1299.99,
  "stock": 25,
  "activo": true,
  "disponible": true,
  "categoria": "Electrónica"
}
```

#### Response Error (404 Not Found)
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Producto no encontrado con ID: 507f1f77bcf86cd799439011",
  "path": "/api/v1/productos/507f1f77bcf86cd799439011",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Ejemplo cURL
```bash
curl -X GET http://localhost:8080/api/v1/productos/507f1f77bcf86cd799439011
```

---

### 3. Búsqueda Polimórfica

**GET** `/api/v1/productos/buscar`

Endpoint flexible que permite búsqueda por diferentes criterios con prioridad específica.

#### Parámetros de Query
| Parámetro | Tipo | Prioridad | Descripción |
|-----------|------|-----------|-------------|
| nombre | String | 1 | Busca por nombre exacto |
| categoria | String | 2 | Filtra por categoría |
| precioMin | BigDecimal | 3 | Precio mínimo del rango |
| precioMax | BigDecimal | 3 | Precio máximo del rango |

#### Casos de Uso

**Búsqueda por nombre:**
```bash
curl -X GET "http://localhost:8080/api/v1/productos/buscar?nombre=Laptop%20Gaming"
```

**Búsqueda por categoría:**
```bash
curl -X GET "http://localhost:8080/api/v1/productos/buscar?categoria=Electrónica"
```

**Búsqueda por rango de precio:**
```bash
curl -X GET "http://localhost:8080/api/v1/productos/buscar?precioMin=1000&precioMax=2000"
```

**Sin parámetros (todos los activos):**
```bash
curl -X GET http://localhost:8080/api/v1/productos/buscar
```

---

### 4. Reducción de Stock

**POST** `/api/v1/productos/{id}/reducir-stock`

Reduce el stock de un producto con validación de concurrencia y disponibilidad.

#### Parámetros de Path
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| id | String | ID único del producto |

#### Parámetros de Query
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| cantidad | Integer | Sí | Cantidad a reducir (mínimo 1) |

#### Response Exitoso (200 OK)
```json
{
  "id": "507f1f77bcf86cd799439011",
  "nombre": "Laptop Gaming",
  "precio": 1299.99,
  "stock": 20,
  "activo": true,
  "disponible": true,
  "categoria": "Electrónica"
}
```

#### Response Error (400 Bad Request)
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Stock insuficiente. Disponible: 5, Solicitado: 10",
  "path": "/api/v1/productos/507f1f77bcf86cd799439011/reducir-stock",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Ejemplo cURL
```bash
curl -X POST "http://localhost:8080/api/v1/productos/507f1f77bcf86cd799439011/reducir-stock?cantidad=5"
```

---

### 5. Streaming en Tiempo Real

**GET** `/api/v1/productos/stream`

Proporciona un flujo continuo de productos activos mediante Server-Sent Events (SSE).

#### Response
```http
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

data: {"id":"507f1f77bcf86cd799439011","nombre":"Laptop Gaming",...}

data: {"id":"507f1f77bcf86cd799439012","nombre":"Mouse Gaming",...}
```

#### Características
- **Emisión cada 2 segundos**: Para demostración
- **Non-blocking**: No bloquea el event loop
- **Auto-reconnect**: Los clientes pueden reconectarse automáticamente

#### Ejemplo cURL
```bash
curl -N -X GET http://localhost:8080/api/v1/productos/stream
```

---

## API v2 - CRUD Funcional

### 1. Listar Todos los Productos

**GET** `/api/v2/productos`

Recupera todos los productos sin filtrar por estado activo.

#### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "507f1f77bcf86cd799439011",
    "nombre": "Laptop Gaming",
    "activo": true,
    "disponible": true
  },
  {
    "id": "507f1f77bcf86cd799439012",
    "nombre": "Producto Inactivo",
    "activo": false,
    "disponible": false
  }
]
```

---

### 2. Crear Producto

**POST** `/api/v2/productos`

Crea un nuevo producto en la base de datos.

#### Request Body
```json
{
  "nombre": "Nuevo Producto",
  "descripcion": "Descripción del nuevo producto",
  "precio": 99.99,
  "stock": 50,
  "categoria": "Categoría Nueva",
  "activo": true
}
```

#### Response Exitoso (201 Created)
```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v2/productos/507f1f77bcf86cd799439013

{
  "id": "507f1f77bcf86cd799439013",
  "nombre": "Nuevo Producto",
  "precio": 99.99,
  "stock": 50,
  "activo": true,
  "disponible": true,
  "categoria": "Categoría Nueva",
  "fechaCreacion": "2024-01-01T12:00:00.000Z",
  "fechaActualizacion": "2024-01-01T12:00:00.000Z"
}
```

#### Response Error (400 Bad Request)
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El nombre es obligatorio",
  "path": "/api/v2/productos",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Ejemplo cURL
```bash
curl -X POST http://localhost:8080/api/v2/productos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Nuevo Producto",
    "precio": 99.99,
    "stock": 50,
    "categoria": "Categoría Nueva",
    "activo": true
  }'
```

---

### 3. Actualizar Producto

**PUT** `/api/v2/productos/{id}`

Actualiza completamente un producto existente.

#### Request Body
```json
{
  "nombre": "Producto Actualizado",
  "descripcion": "Descripción actualizada",
  "precio": 149.99,
  "stock": 30,
  "categoria": "Categoría Actualizada",
  "activo": true
}
```

#### Response Exitoso (200 OK)
```json
{
  "id": "507f1f77bcf86cd799439011",
  "nombre": "Producto Actualizado",
  "precio": 149.99,
  "stock": 30,
  "activo": true,
  "disponible": true,
  "categoria": "Categoría Actualizada",
  "fechaActualizacion": "2024-01-01T12:30:00.000Z"
}
```

#### Ejemplo cURL
```bash
curl -X PUT http://localhost:8080/api/v2/productos/507f1f77bcf86cd799439011 \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Producto Actualizado",
    "precio": 149.99,
    "stock": 30,
    "activo": true
  }'
```

---

### 4. Eliminar Producto

**DELETE** `/api/v2/productos/{id}`

Elimina permanentemente un producto de la base de datos.

#### Response Exitoso (204 No Content)
```http
HTTP/1.1 204 No Content
```

#### Response Error (404 Not Found)
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Producto no encontrado con ID: 507f1f77bcf86cd799439011",
  "path": "/api/v2/productos/507f1f77bcf86cd799439011",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Ejemplo cURL
```bash
curl -X DELETE http://localhost:8080/api/v2/productos/507f1f77bcf86cd799439011
```

---

## API v2 - Operaciones Masivas

### 1. Actualización Masiva

**PUT** `/api/v2/productos/bulk/update`

Actualiza múltiples productos en una sola operación con concurrencia controlada.

#### Request Body
```json
{
  "productos": [
    {
      "id": "507f1f77bcf86cd799439011",
      "nombre": "Producto 1 Actualizado",
      "precio": 199.99
    },
    {
      "id": "507f1f77bcf86cd799439012",
      "stock": 100,
      "activo": false
    }
  ]
}
```

#### Response Exitoso (200 OK)
```json
{
  "operation": "BULK_UPDATE",
  "successCount": 1,
  "failedCount": 1,
  "successIds": ["507f1f77bcf86cd799439011"],
  "errors": [
    {
      "id": "507f1f77bcf86cd799439012",
      "message": "Producto no encontrado"
    }
  ]
}
```

#### Características
- **Concurrencia controlada**: Máximo 32 operaciones simultáneas
- **Partial failure**: Algunas operaciones pueden fallar sin afectar a otras
- **Resultados detallados**: Informe completo de éxitos y fallos

#### Ejemplo cURL
```bash
curl -X PUT http://localhost:8080/api/v2/productos/bulk/update \
  -H "Content-Type: application/json" \
  -d '{
    "productos": [
      {
        "id": "507f1f77bcf86cd799439011",
        "precio": 199.99
      },
      {
        "id": "507f1f77bcf86cd799439012",
        "stock": 100
      }
    ]
  }'
```

---

## Manejo de Errores

### Estructura de Error Estándar
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensaje descriptivo del error",
  "path": "/path/del/endpoint",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Tipos de Errores Comunes

| Status Code | Tipo | Descripción |
|-------------|------|-------------|
| 400 | Bad Request | Validación fallida, parámetros incorrectos |
| 404 | Not Found | Recurso no encontrado |
| 422 | Unprocessable Entity | Error de validación de negocio |
| 500 | Internal Server Error | Error inesperado del servidor |

### Errores de Validación
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v2/productos",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "validationErrors": [
    {
      "field": "nombre",
      "message": "El nombre es obligatorio"
    },
    {
      "field": "precio",
      "message": "El precio debe ser mayor a 0"
    }
  ]
}
```

---

## Consideraciones de Rendimiento

### Streaming y SSE
- Los endpoints de streaming mantienen conexiones abiertas
- Configurar timeouts apropiados en clientes
- Manejar reconexiones automáticamente

### Operaciones Masivas
- Límite de concurrencia: 32 operaciones simultáneas
- Timeout configurado para operaciones largas
- Monitorear métricas de bulk operations

### Caching
- Considerar implementar caché para productos frecuentes
- Los endpoints de búsqueda pueden beneficiarse de caché
- Invalidación de caché en actualizaciones

---

## Ejemplos de Integración

### JavaScript (Fetch)
```javascript
// Listar productos activos
const response = await fetch('/api/v1/productos/activos');
const productos = await response.json();

// Crear producto
const nuevoProducto = {
  nombre: "Producto JS",
  precio: 99.99,
  stock: 10,
  categoria: "Electrónica",
  activo: true
};

const createResponse = await fetch('/api/v2/productos', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(nuevoProducto)
});
```

### Python (requests)
```python
import requests

# Obtener producto por ID
response = requests.get('http://localhost:8080/api/v1/productos/507f1f77bcf86cd799439011')
producto = response.json()

# Actualización masiva
bulk_data = {
    "productos": [
        {"id": "507f1f77bcf86cd799439011", "precio": 199.99},
        {"id": "507f1f77bcf86cd799439012", "stock": 50}
    ]
}

response = requests.put(
    'http://localhost:8080/api/v2/productos/bulk/update',
    json=bulk_data
)
result = response.json()
```

Esta API reference proporciona toda la información necesaria para integrarse con los servicios de WebFlux Demo.
