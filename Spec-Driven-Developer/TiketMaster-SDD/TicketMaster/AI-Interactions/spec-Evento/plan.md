# Plan de implementación — Evento (TicketMaster)

> Fecha: 2026-11-22
> Spec relacionada: `AI-Interactions/spec-Evento/spec-Evento.md`
> Destino: `AI-Interactions/spec-Evento/plan.md`
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
- `DELETE /eventos/{id:\d+}` devuelve **`200 OK` con `{ mensaje, evento }`** tanto al dar de baja como si ya estaba Eliminado.
- Las rutas con ID se restringen con `{id:\d+}` para que `/eventos/eliminados` nunca colisione con `/eventos/{id}`.
- Tests: fuera de alcance de este plan.

## 2. Orden de implementación

- [ ] Agregar `spring-boot-starter-validation` a `pom.xml` (autorización registrada; único cambio permitido en ese archivo).
- [ ] Actualizar `AI-Interactions/spec-Evento/spec-Evento.md`: quitar la regla de unicidad por título + inicio + ubicación, el caso de error asociado y los dos criterios de aceptación de duplicados/Eliminado-libera.
- [ ] Crear `src/main/resources/db/migration/V1__create_table_eventos.sql` con la DDL de la sección 3.
- [ ] Crear `evento/entity/Evento.java`: entidad JPA sobre la tabla `eventos`, con `@Id` + `@GeneratedValue(strategy = IDENTITY)` y `@Enumerated(EnumType.STRING)` para el estado.
- [ ] Crear `evento/entity/EventoRepository.java`: `JpaRepository<Evento, Long>` con una única query combinada de filtros opcionales (estado declarado/efectivo, rango de fechas sobre `inicio` y título; null se ignora) usando `:ahora`, más la consulta explícita de Eliminados.
- [ ] Crear `evento/service/EventoDTORequest.java` como `record` con `@NotBlank` en título, descripción, ubicación y registrador; `@NotNull` en inicio, fin y aforo; `@Min(1)` en aforo.
- [ ] Crear `evento/service/EventoDTOResponse.java` como `record` con: id, los mismos datos y `estado` efectivo.
- [ ] Crear `evento/service/MensajeEventoResponse.java` como `record` con `mensaje` y `evento`, usado por el DELETE.
- [ ] Crear `evento/service/EventoDTOMapper.java` con `toDomain(EventoDTORequest)` → `Evento` seteando `estado = BORRADOR` y dejando `id` en null; y `toResponse(Evento, LocalDateTime ahora)` → `EventoDTOResponse` calculando el estado efectivo con el mismo reloj.
- [ ] Crear `evento/exception/EventoNotFoundException.java` y `evento/exception/EventoBusinessException.java`.
- [ ] Crear `evento/service/EventoService.java` con los métodos de la sección 5.
- [ ] Crear `evento/controller/EventoController.java` sin lógica: `@Valid` en el body, `ResponseEntity.status(HttpStatus.CREATED).body(...)` en el POST y llamadas al Service.
- [ ] Crear `evento/controller/EventoExceptionHandler.java` con `@RestControllerAdvice` para traducir las excepciones a respuesta con mensaje.

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
- Normalización del parámetro `estado`: acepta texto legible (`Borrador`, `Publicado`, `En curso`, `Finalizado`, `Cancelado`, `Eliminado`) y enum (`BORRADOR`, `PUBLICADO`, `EN_CURSO`, `FINALIZADO`, `CANCELADO`, `ELIMINADO`), sin distinguir mayúsculas, espacios ni guiones. La hace el Service, no el controller. Un valor inválido se rechaza con error 400 y mensaje claro. El resultado se separa en `estadoDeclarado` (`BORRADOR`/`CANCELADO`/`ELIMINADO`) o `estadoEfectivo` (`PUBLICADO`/`EN_CURSO`/`FINALIZADO`); si no viene estado, ambos quedan null.
- `buscarPorId(Long id)` → si no existe, lanza `EventoNotFoundException`.
- `listarEliminados()` → consulta explícita separada.
- No existe `buscarPorTitulo`: la búsqueda por título se hace dentro de `listar(...)`.
- No existe `editar(...)` ni validación de duplicados.

## 6. Endpoints del controller

| Método | Ruta | Respuesta |
| --- | --- | --- |
| `POST` | `/eventos` | `ResponseEntity.status(HttpStatus.CREATED).body(...)`. **Sin `ServletUriComponentsBuilder`**. |
| `POST` | `/eventos/{id:\d+}/publicar` | 200 con `EventoDTOResponse` actualizado |
| `DELETE` | `/eventos/{id:\d+}` | 200 con `{ mensaje, evento }` en ambos casos (baja lógica) |
| `GET` | `/eventos` | 200 con listado; parámetros `estado` (incluye `Eliminado`), `desde`, `hasta` y `titulo`; normalización en el Service |
| `GET` | `/eventos/{id:\d+}` | 200 con `EventoDTOResponse` / 404 si no existe |
| `GET` | `/eventos/eliminados` | 200 con eliminados (ruta literal, no choca con `{id:\d+}`) |

El controller solo recibe DTOs, valida con `@Valid`, llama al Service y devuelve la respuesta indicada.

## 7. Fuera de alcance

- Edición de eventos.
- Validación de duplicados por título + inicio + ubicación (retirada de la spec).
- Entidad Ticket y registro de tickets.
- Autenticación, roles o control de acceso.
- Tests.
- Modificación de `application.properties` o de cualquier otro archivo protegido.

## Checklist

- [ ] Leí la spec y este plan antes de implementar.
- [ ] `pom.xml` se modificó solo para agregar `spring-boot-starter-validation`.
- [ ] La migración se creó con `V1__create_table_eventos.sql` y no se editó después de aplicarla.
- [ ] La entidad y el repositorio quedaron en `evento/entity/`.
- [ ] No se usó `NOW()` de MySQL: el mismo `LocalDateTime.now()` se pasó a repositorio y mapper.
- [ ] La query combinada ignora los filtros que llegan en null; `estado=Eliminado` incluye eliminados y sin filtro se excluyen.
- [ ] `EventoDTORequest` usa `jakarta.validation` y el controller usa `@Valid`.
- [ ] Las reglas de fechas y estados se validan en el Service.
- [ ] `POST /eventos` devuelve 201 con `HttpStatus.CREATED`, sin `ServletUriComponentsBuilder`.
- [ ] Las rutas con ID usan `{id:\d+}`.
- [ ] `DELETE` devuelve `{ mensaje, evento }` con 200 en ambos casos.
- [ ] `toDomain` setea el estado inicial y `toResponse` devuelve el estado efectivo.
- [ ] El ID es autoincrement; no hay método `editar` ni validación de duplicados.
- [ ] Los errores pasan por excepciones de dominio y el `@RestControllerAdvice`.
- [ ] No se implementó la entidad Ticket ni autenticación.
- [ ] No se modificaron `application.properties` ni zonas protegidas.
- [ ] Marqué cada ítem solo después de verificarlo.
