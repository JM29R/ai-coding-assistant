# Plan de implementación — Evento (TicketMaster)

> Fecha: 2026-11-22
> Spec relacionada: `AI-Interactions/spec-Evento/spec-Evento.md`
> Destino: `AI-Interactions/spec-Evento/plan-Evento.md`
> Contenido técnico. Las reglas de negocio están en la spec; este plano solo dice cómo se implementan.

## 1. Decisiones técnicas

- Stack existente: Java 21, Spring Boot 4.0.10, spring-webmvc, spring-data-jpa, Flyway + flyway-mysql, Lombok, MySQL. En `pom.xml` el único cambio autorizado es agregar `spring-boot-starter-validation`.
- Base de datos: tabla **`eventos`**, migración Flyway **`V1__create_table_eventos.sql`** en `src/main/resources/db/migration/`. No se edita una migración una vez aplicada.
- Módulo: `src/main/java/JMR/SDD/TicketMaster/evento/` con subpaquetes `entity`, `service`, `controller` y `exception`.
- Identificador: `Long` en Java / `BIGINT AUTO_INCREMENT` en MySQL, generado por la BD.
- Estado: enum Java `EventoEstado` (`BORRADOR`, `PUBLICADO`, `CANCELADO`, `ELIMINADO`) persistido como `VARCHAR(20)`.
- Estados `En curso` y `Finalizado` NO se persisten: se calculan comparando la fecha/hora actual con `inicio` y `fin`.
- **Reloj unificado**: el `EventoService` obtiene `LocalDateTime.now()` una sola vez por operación y lo pasa como parámetro `:ahora` al repositorio y al mapper. **No se usa `NOW()` de MySQL**, para evitar desfases de zona horaria entre JVM y MySQL.
- **Validación**: `spring-boot-starter-validation` + anotaciones `jakarta.validation` en `EventoDTORequest` (`@NotBlank`, `@NotNull`, `@Min(1)`) y `@Valid` en el controller. Las reglas de fechas y de estados se validan en el `EventoService`.
- El filtrado por estado efectivo se resuelve **en SQL** usando el parámetro `:ahora`.
- No se implementa validación de duplicados ni operación de edición (la regla de unicidad se retiró de la spec).
- Errores de negocio: excepciones propias + `@RestControllerAdvice`. El controller no contiene lógica.
- `DELETE /eventos/{id}` devuelve **`200 OK` con `{ mensaje, evento }`** tanto al dar de baja como si ya estaba Eliminado.
- Las rutas con ID usan `@PathVariable Long id` sobre la ruta literal `/{id}`. Las rutas literales (`/eventos/publicar/{id}`, `/eventos/todos` y `/eventos/eliminados`) se resuelven antes que `/eventos/{id}`, así que no hay colisiones.
- Tests: fuera de alcance de este plan.

## 2. Orden de implementación

- [x] Agregar `spring-boot-starter-validation` a `pom.xml` (autorización registrada; único cambio permitido en ese archivo).
- [x] Actualizar `AI-Interactions/spec-Evento/spec-Evento.md`: quitar la regla de unicidad por título + inicio + ubicación, el caso de error asociado y los dos criterios de aceptación de duplicados/Eliminado-libera.
- [x] Crear `src/main/resources/db/migration/V1__create_table_eventos.sql` con la DDL de la sección 3.
- [x] Crear `evento/entity/Evento.java`: entidad JPA sobre la tabla `eventos`, con `@Id` + `@GeneratedValue(strategy = IDENTITY)` y `@Enumerated(EnumType.STRING)` para el estado.
- [x] Crear `evento/entity/EventoRepository.java`: `JpaRepository<Evento, Long>` con una única query combinada de filtros opcionales (estado declarado/efectivo, rango de fechas por solapamiento y título; null se ignora) usando `:ahora`, más la consulta explícita de Eliminados.
- [x] Crear `evento/service/EventoDTORequest.java` como `record` con `@NotBlank` en título, descripción, ubicación y registrador; `@NotNull` en inicio, fin y aforo; `@Min(1)` en aforo.
- [x] Crear `evento/service/EventoDTOResponse.java` como `record` con: id, los mismos datos y `estado` efectivo.
- [x] Crear `evento/service/MensajeEventoResponse.java` como `record` con `mensaje` y `evento`, usado por el DELETE.
- [x] Crear `evento/service/EventoDTOMapper.java` con `toDomain(EventoDTORequest)` → `Evento` seteando `estado = BORRADOR` y dejando `id` en null; y `toResponse(Evento, LocalDateTime ahora)` → `EventoDTOResponse` calculando el estado efectivo con el mismo reloj.
- [x] Crear `evento/exception/EventoNotFoundException.java` y `evento/exception/EventoBusinessException.java`.
- [x] Crear `evento/service/EventoService.java` con los métodos de la sección 5.
- [x] Crear `evento/controller/EventoController.java` sin lógica: `@Valid` en el body, `ResponseEntity.status(HttpStatus.CREATED).body(...)` en el POST y llamadas al Service.
- [x] Crear `evento/controller/EventoExceptionHandler.java` con `@RestControllerAdvice` para traducir las excepciones a respuesta con mensaje.

## 3. Migración propuesta

```sql
CREATE TABLE eventos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    inicio DATETIME NOT NULL,
    fin DATETIME NOT NULL,
    ubicacion VARCHAR(255) NOT NULL,
    aforo INT NOT NULL,
    registrador VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_eventos_aforo CHECK (aforo >= 1),
    CONSTRAINT chk_eventos_fin CHECK (fin > inicio),
    CONSTRAINT chk_eventos_estado CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'CANCELADO', 'ELIMINADO'))
);
```

Longitudes propuestas (título 200, ubicación 255, registrador 100) son ajustables antes de escribir la migración.

## 4. Consultas y estados calculados en SQL

- El repositorio recibe `@Param("ahora") LocalDateTime ahora` en todas las consultas de estado efectivo. **No se usa `NOW()` de MySQL.**
- Listado normal: `estado <> 'ELIMINADO'`.
- Si el parámetro de estado llega explícitamente como Eliminado, el listado incluye los Eliminados; sin filtro, se excluyen.
- Consulta explícita de eliminados: `estado = 'ELIMINADO'`.
- Búsqueda por título: coincidencia parcial, sin distinguir mayúsculas.
- Rango de fechas por solapamiento: `desde` → `e.fin >= :desde`; `hasta` → `e.inicio <= :hasta`. Devuelve los eventos que se cruzan con el rango, no solo los que empiezan dentro.
- Filtro `Publicado`: `estado = 'PUBLICADO' AND :ahora < inicio`.
- Filtro `En curso`: `estado = 'PUBLICADO' AND :ahora BETWEEN inicio AND fin`.
- Filtro `Finalizado`: `estado = 'PUBLICADO' AND :ahora > fin`.
- Filtros declarados (`BORRADOR`, `CANCELADO`, `ELIMINADO`): comparación directa de `estado`.
- El `EventoService` obtiene `ahora = LocalDateTime.now()` una vez por operación y lo reutiliza al llamar al repositorio y al mapper, de modo que filtro y respuesta usen el mismo instante.
- El estado efectivo devuelto en `EventoDTOResponse` se calcula con la misma comparación de fechas: `Publicado`, `En curso`, `Finalizado`, o el estado declarado (`Borrador`, `Cancelado`, `Eliminado`).

## 5. EventoService

- `crear(EventoDTORequest)` → la validación Bean Validation ya se aplicó en el controller; después valida `fin > inicio` e `inicio` futuro con el `ahora` de la operación; vía mapper setea `BORRADOR`; guarda y devuelve `EventoDTOResponse`.
- `publicar(Long id)` → valida existencia, estado `BORRADOR` e `inicio` futuro con el mismo `ahora`; cambia a `PUBLICADO`. Si no existe o no cumple, lanza excepción de negocio.
- `darDeBaja(Long id)` → cambia el estado a `ELIMINADO` sin borrar el registro y devuelve `MensajeEventoResponse`: `"Evento dado de baja"` si cambió, o `"El evento ya estaba dado de baja"` si ya estaba Eliminado (sin cambios). En ambos casos el DTO incluido queda con estado `Eliminado`.
- `listar(String estado, LocalDateTime desde, LocalDateTime hasta, String titulo)` → única operación de consulta con filtros opcionales; null se ignora. `estado=Eliminado` incluye eliminados; sin filtro, se excluyen.
- `listarTodos()` → delega en `listar(null, null, null, null)`: devuelve el listado sin filtros y **sin Eliminados**, igual que el listado normal.
- Normalización del parámetro `estado`: acepta texto legible (`Borrador`, `Publicado`, `En curso`, `Finalizado`, `Cancelado`, `Eliminado`) y enum (`BORRADOR`, `PUBLICADO`, `EN_CURSO`, `FINALIZADO`, `CANCELADO`, `ELIMINADO`), sin distinguir mayúsculas, espacios ni guiones. La hace el Service, no el controller. Un valor inválido se rechaza con error 400 y mensaje claro. El resultado se separa en `estadoDeclarado` (`BORRADOR`/`CANCELADO`/`ELIMINADO`) o `estadoEfectivo` (`PUBLICADO`/`EN_CURSO`/`FINALIZADO`); si no viene estado, ambos quedan null.
- `buscarPorId(Long id)` → si no existe, lanza `EventoNotFoundException`.
- `listarEliminados()` → consulta explícita separada, implementada reutilizando `listarConFiltros` con `estadoDeclarado = ELIMINADO`.
- No existe `buscarPorTitulo`: la búsqueda por título se hace dentro de `listar(...)`.
- No existe `editar(...)` ni validación de duplicados.

## 6. Endpoints del controller

| Método | Ruta | Respuesta |
| --- | --- | --- |
| `POST` | `/eventos` | `ResponseEntity.status(HttpStatus.CREATED).body(...)`. **Sin `ServletUriComponentsBuilder`**. |
| `POST` | `/eventos/publicar/{id}` | 200 con `EventoDTOResponse` actualizado; `@PathVariable Long id` |
| `DELETE` | `/eventos/{id}` | 200 con `{ mensaje, evento }` en ambos casos (baja lógica); `@PathVariable Long id` |
| `GET` | `/eventos` | 200 con listado; parámetros `estado` (incluye `Eliminado`), `desde`, `hasta` y `titulo`; normalización en el Service |
| `GET` | `/eventos/todos` | 200 con `listarTodos()`: sin filtros y sin Eliminados |
| `GET` | `/eventos/{id}` | 200 con `EventoDTOResponse` / 404 si no existe; `@PathVariable Long id` |
| `GET` | `/eventos/eliminados` | 200 con eliminados (ruta literal, resuelta antes que `/{id}`) |

El controller solo recibe DTOs, valida con `@Valid`, llama al Service y devuelve la respuesta indicada.

## 7. Fuera de alcance

- Edición de eventos.
- Validación de duplicados por título + inicio + ubicación (retirada de la spec).
- Entidad Ticket y registro de tickets.
- Autenticación, roles o control de acceso.
- Tests.
- Modificación de `application.properties` o de cualquier otro archivo protegido.

## Checklist

- [x] Leí la spec y este plan antes de implementar.
- [x] `pom.xml` se modificó solo para agregar `spring-boot-starter-validation`.
- [x] La migración se creó con `V1__create_table_eventos.sql` y no se editó después de aplicarla.
- [x] La entidad y el repositorio quedaron en `evento/entity/`.
- [x] No se usó `NOW()` de MySQL: el mismo `LocalDateTime.now()` se pasó a repositorio y mapper.
- [x] La query combinada ignora los filtros que llegan en null; `estado=Eliminado` incluye eliminados y sin filtro se excluyen.
- [x] `EventoDTORequest` usa `jakarta.validation` y el controller usa `@Valid`.
- [x] Las reglas de fechas y estados se validan en el Service.
- [x] `POST /eventos` devuelve 201 con `HttpStatus.CREATED`, sin `ServletUriComponentsBuilder`.
- [x] Las rutas de publicar, eliminar y buscar por ID usan `@PathVariable` sobre `/{id}`.
- [x] `GET /eventos/todos` y `listarTodos()` devuelven el listado sin filtros y sin Eliminados.
- [x] `DELETE` devuelve `{ mensaje, evento }` con 200 en ambos casos.
- [x] `toDomain` setea el estado inicial y `toResponse` devuelve el estado efectivo.
- [x] El ID es autoincrement; no hay método `editar` ni validación de duplicados.
- [x] Los errores pasan por excepciones de dominio y el `@RestControllerAdvice`.
- [x] No se implementó la entidad Ticket ni autenticación.
- [x] No se modificaron `application.properties` ni zonas protegidas.
- [x] Marqué cada ítem solo después de verificarlo.
    