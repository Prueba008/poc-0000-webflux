package com.example.webflux.service;

import com.example.webflux.exception.ConflictException;
import com.example.webflux.exception.NotFoundException;
import com.example.webflux.model.ProductDoc;
import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.model.dto.producto.ProductoResponse;
import com.example.webflux.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductoRepository productoRepository;
    private final ReactiveMongoTemplate mongoTemplate;

    // --- Consultas de Lectura ---

    @Override
    public Flux<ProductoResponse> findDisponibles() {
        return productoRepository.findDisponibles()
                .map(ProductoResponse::fromEntity);
    }

    @Override
    public Flux<ProductoResponse> findAllActivos() {
        return productoRepository.findByActivoTrue()
                .map(ProductoResponse::fromEntity);
    }

    @Override
    public Mono<ProductoResponse> findById(String id) {
        return productoRepository.findById(id)
                .map(ProductoResponse::fromEntity)
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado: " + id)));
    }

    @Override
    public Flux<ProductoResponse> findByNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre)
                .map(ProductoResponse::fromEntity);
    }

    @Override
    public Flux<ProductoResponse> findByCategoria(String categoria) {
        return productoRepository.findByCategoriaIgnoreCase(categoria)
                .map(ProductoResponse::fromEntity);
    }

    @Override
    public Flux<ProductoResponse> findByPrecioRange(BigDecimal min, BigDecimal max) {
        return productoRepository.findByPrecioBetweenAndActivoTrue(min, max)
                .map(ProductoResponse::fromEntity);
    }

    // --- Operaciones de Escritura ---

    @Override
    public Mono<ProductoResponse> create(ProductoRequest req) {
        return productoRepository.findByNombreIgnoreCase(req.getNombre())
                .flatMap(exists -> Mono.<Producto>error(new ConflictException("El nombre ya existe")))
                .switchIfEmpty(Mono.defer(() -> save(req)))
                .map(ProductoResponse::fromEntity);
    }

    /**
     * CORRECCIÓN: El método debe recibir ProductoRequest para persistencia.
     * Retorna la entidad para uso interno o composición.
     *
     * @param request
     */
    @Override
    public Mono<Producto> save(ProductoResponse request) {
        return null;
    }


    /**
     * Lógica centralizada de persistencia.
     * Recibe ProductoRequest para asegurar que el contrato de entrada sea el correcto.
     */

    public Mono<Producto> save(ProductoRequest request) { // 1. Corregido de Response a Request
        return Mono.just(request)
                .flatMap(this::validateRequest)          // 2. Validación reactiva
                .map(this::mapToEntity)                  // 3. Conversión DTO -> Entidad
                .flatMap(p -> {                          // 4. Persistencia con Auditoría
                    Instant now = Instant.now();
                    if (p.getId() == null) {
                        p.setFechaCreacion(now);
                        if (p.getActivo() == null) {
                            p.setActivo(true);
                        }
                    }
                    p.setFechaActualizacion(now);
                    return productoRepository.save(p);
                })
                .doOnSuccess(p -> log.info("Producto guardado exitosamente con ID: {}", p.getId()));
    }
    @Override
    public Mono<ProductoResponse> update(String id, ProductoRequest req) {
        return productoRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado")))
                .flatMap(p -> {
                    updateEntityWithRequest(p, req);
                    p.setFechaActualizacion(Instant.now());
                    return productoRepository.save(p);
                })
                .map(ProductoResponse::fromEntity);
    }

    @Override
    public Mono<ProductoResponse> patch(String id, ProductoRequest req) {
        return update(id, req); // Reutiliza lógica de actualización parcial
    }

    @Override
    public Mono<Void> softDelete(String id) {
        return productoRepository.findById(id)
                .flatMap(p -> {
                    p.setActivo(false);
                    p.setFechaActualizacion(Instant.now());
                    return productoRepository.save(p);
                })
                .then();
    }

    // --- Operaciones Bulk ---

    @Override
    public Flux<Producto> saveAll(Collection<Producto> productos) {
        return Flux.fromIterable(productos)
                .map(p -> {
                    Instant now = Instant.now();
                    p.setFechaCreacion(now);
                    p.setFechaActualizacion(now);
                    if (p.getActivo() == null) p.setActivo(true);
                    return p;
                })
                .collectList()
                .flatMapMany(productoRepository::saveAll);
    }

    @Override
    public Flux<Producto> updateAll(Collection<Producto> productos) {
        return productoRepository.saveAll(productos);
    }

    @Override
    public Flux<Producto> updateActivoAll(Collection<String> ids, Boolean activo) {
        return productoRepository.findAllById(ids)
                .flatMap(p -> {
                    p.setActivo(activo);
                    p.setFechaActualizacion(Instant.now());
                    return productoRepository.save(p);
                });
    }

    @Override
    public Flux<Producto> deactivateAll(Collection<String> ids) {
        return updateActivoAll(ids, false);
    }

    @Override
    public Mono<Long> deleteAllById(Collection<String> ids) {
        return productoRepository.deleteAllById(ids)
                .then(Mono.fromSupplier(() -> (long) ids.size()));
    }

    // --- Conteos ---

    @Override
    public Mono<Long> countActivos() {
        return countByActivo(true);
    }

    @Override
    public Mono<Long> countByActivo(boolean activo) {
        return productoRepository.countByActivo(activo);
    }

    // --- Stock y Atómicos ---

    @Override
    public Mono<ProductoResponse> reducirStock(String id, Integer cantidad) {
        return productoRepository.findById(id)
                .flatMap(p -> {
                    int result = p.getStock() - cantidad;
                    if (result < 0) return Mono.error(new IllegalArgumentException("Stock insuficiente"));
                    p.setStock(result);
                    p.setFechaActualizacion(Instant.now());
                    return productoRepository.save(p);
                })
                .map(ProductoResponse::fromEntity);
    }

    @Override
    public Mono<ProductDoc> updateAtomic(String id, ProductDoc r) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("sku", r.getSku())
                .set("name", r.getName())
                .set("price", r.getPrice())
                .set("fechaActualizacion", Instant.now());

        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), ProductDoc.class);
    }

    // --- Helpers de Mapeo y Validación ---

    private void updateEntityWithRequest(Producto p, ProductoRequest req) {
        if (req.getNombre() != null) p.setNombre(req.getNombre());
        if (req.getDescripcion() != null) p.setDescripcion(req.getDescripcion());
        if (req.getPrecio() != null) p.setPrecio(req.getPrecio());
        if (req.getStock() != null) p.setStock(req.getStock());
        if (req.getCategoria() != null) p.setCategoria(req.getCategoria());
        if (req.getActivo() != null) p.setActivo(req.getActivo());
    }

    private Producto mapToEntity(ProductoRequest req) {
        return Producto.builder()
                .nombre(req.getNombre())
                .descripcion(req.getDescripcion())
                .precio(req.getPrecio())
                .stock(req.getStock())
                .categoria(req.getCategoria())
                .activo(req.getActivo())
                .build();
    }

    private Mono<ProductoRequest> validateRequest(ProductoRequest req) {
        if (req.getPrecio() != null && req.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            return Mono.error(new IllegalArgumentException("Precio negativo"));
        }
        return Mono.just(req);
    }
}