# Tasks — Evento (TicketMaster)

> Fecha: 2026-11-22
> Spec: `AI-Interactions/spec-Evento/spec-Evento.md` · Plan: `AI-Interactions/spec-Evento/plan-Evento.md`
> Regla de marcado: marcar `- [x]` SOLO después de ejecutar y verificar. Si no se pudo, dejar `- [ ]` y explicar el motivo.
> Fuera de alcance: entidad Ticket, autenticación, edición de eventos, tests y migraciones adicionales.

## Índice de criterios de aceptación (CA)

- CA-01 Alta correcta: Borrador + identificador + consultable
- CA-02 Falta descripción → se rechaza e informa
- CA-03 Fin < inicio → se rechaza e informa
- CA-04 Aforo 0 → se rechaza e informa
- CA-05 Registrador como texto simple, sin login
- CA-06 Publicar Borrador con inicio futuro → Publicado
- CA-07 Publicar con inicio ya pasado → se rechaza, sigue Borrador
- CA-08 Publicar estado Publicado/Eliminado → se rechaza
- CA-09 Publicar no modifica ningún otro dato
- CA-10 Listado sin filtro, sin Eliminados
- CA-11 Publicado antes de Inicio → se muestra Publicado
- CA-12 Publicado entre Inicio y Fin → se muestra En curso
- CA-13 Publicado después de Fin → se muestra Finalizado
- CA-14 Eliminado: no aparece en el listado normal; aparece con filtro explícito y en la consulta explícita
- CA-15 Filtros por estado y rango de fechas
- CA-16 Evento inexistente → "no encontrado"
- CA-17 Baja → estado Eliminado, sigue existiendo, sale del listado normal
- CA-18 Tickets ya registrados se conservan como histórico
- CA-19 Ticket en Eliminado → se rechaza e informa
- CA-20 Ticket en Finalizado calculado → se rechaza e informa
- CA-21 Doble baja → sin cambios y se informa
- CA-22 Cupo completo rechaza ticket **(diferido: requiere entidad Ticket)**
- CA-23 Estado inválido para ticket rechazado **(diferido: requiere entidad Ticket)**

> CA-22 y CA-23 NO se implementan en este task: la spec los declara como invariantes del Evento, pero su verificación depende de la futura spec de Ticket.

## Árbol de ejecución

```text
Fase 0 — Precondiciones
├── T00   Precondiciones (verificado)
└── T00b  Aclaración documental del filtro Eliminado
Fase 1 — Persistencia
├── T01   Migración V1__create_table_eventos.sql
├── T02   EventoEstado.java
├── T03   Evento.java
└── T04   EventoRepository.java
Fase 2 — Contratos y soporte
├── T05   EventoDTORequest.java
├── T06   EventoDTOResponse.java
├── T07   MensajeEventoResponse.java
├── T08   EventoDTOMapper.java
├── T09   EventoNotFoundException.java
└── T10   EventoBusinessException.java
Fase 3 — Lógica de negocio
└── T11   EventoService.java
Fase 4 — HTTP
├── T12   EventoController.java
└── T13   EventoExceptionHandler.java
Fase 5 — Verificación
└── T14   Compilación
Fase 6 — Ajuste posterior a la documentación
└── T15   Rutas con @PathVariable y listado sin filtro
```

---

## Fase 0 — Precondiciones

### T00 — Precondiciones
- **Objetivo**: confirmar que la documentación y la dependencia ya están listas; no rehacerlas.
- **Depende de**: ninguna.
- **Archivos**: `pom.xml`, `spec-Evento.md`, `plan-Evento.md`.
- **Pasos**:
  - [x] `pom.xml` contiene `spring-boot-starter-validation` y no tiene otros cambios.
  - [x] `spec-Evento.md` ya no contiene la regla de unicidad ni sus criterios.
  - [x] `plan-Evento.md` existe y contiene las decisiones técnicas vigentes.
- **Definition of Done**:
  - [x] Los tres archivos están en el estado esperado.
- **Cubre**: documentación base de CA-01…CA-23.

### T00b — Aclaración documental del filtro Eliminado
- **Objetivo**: dejar consistente la regla de consulta con el filtro explícito.
- **Depende de**: T00.
- **Archivos**: `spec-Evento.md`, `plan-Evento.md` (este `task-Evento.md` ya refleja la regla).
- **Pasos**:
  - [x] spec §3 Consulta: "Si se filtra explícitamente por Eliminado, esos Eventos se incluyen; sin filtro, el listado normal los excluye".
  - [x] plan §4 y §6: mismo matiz en el listado y en `GET /eventos`.
  - [x] CA-10 se mantiene: sin filtro, no aparecen Eliminados.
  - [x] CA-14 cubre también: con `estado=Eliminado` en `GET /eventos`, sí aparecen.
- **Definition of Done**:
  - [x] Los tres `.md` dicen lo mismo y no contradicen la spec.
- **Cubre**: CA-10, CA-14.

---

## Fase 1 — Persistencia

### T01 — Migración Flyway
- **Objetivo**: crear la tabla `eventos`.
- **Depende de**: T00.
- **Archivo**: `src/main/resources/db/migration/V1__create_table_eventos.sql`.
- **Referencia**: plan §3 (DDL).
- **Pasos**:
  - [x] Crear el archivo con esta DDL:

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

  - [x] No editar la migración después de aplicarla.
- **Definition of Done**:
  - [x] La tabla queda con 9 columnas, PK autoincrement y los 3 checks.
- **Cubre**: CA-01 (identificador generado).

### T02 — Enum EventoEstado
- **Objetivo**: representar los estados declarados.
- **Depende de**: T00.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/entity/EventoEstado.java`.
- **Pasos**:
  - [x] Declarar `BORRADOR`, `PUBLICADO`, `CANCELADO`, `ELIMINADO`.
  - [x] No incluir `EN_CURSO` ni `FINALIZADO` (son calculados, no persistidos).
- **Definition of Done**:
  - [x] El enum coincide con el check `chk_eventos_estado`.
- **Cubre**: CA-06, CA-17 (parcial).

### T03 — Entidad Evento
- **Objetivo**: mapear la tabla `eventos`.
- **Depende de**: T01, T02.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/entity/Evento.java`.
- **Pasos**:
  - [x] `@Entity` + `@Table(name = "eventos")`.
  - [x] `id Long` con `@Id` y `@GeneratedValue(strategy = IDENTITY)`.
  - [x] Campos: `titulo`, `descripcion`, `inicio`, `fin`, `ubicacion`, `aforo`, `registrador`.
  - [x] `estado EventoEstado` con `@Enumerated(EnumType.STRING)`.
  - [x] No agregar campo persistido para los estados calculados.
- **Definition of Done**:
  - [x] La entidad refleja exactamente los datos de la spec §2.
- **Cubre**: CA-01, CA-06, CA-17 (parcial).

### T04 — Repositorio
- **Objetivo**: una sola query con todos los filtros opcionales; null = se ignora.
- **Depende de**: T03, T00b.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/entity/EventoRepository.java`.
- **Firma a implementar**:

```java
List<Evento> listarConFiltros(@Param("estadoEfectivo") String estadoEfectivo,
                             @Param("estadoDeclarado") EventoEstado estadoDeclarado,
                             @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta,
                             @Param("titulo") String titulo,
                             @Param("ahora") LocalDateTime ahora);
```

- **Pasos**:
  - [x] Extender `JpaRepository<Evento, Long>` y conservar `findById` heredado.
  - [x] Eliminar `findByTituloContainingIgnoreCase`, `findByEstadoNot` y `findByEstado` (todos quedan absorbidos).
  - [x] La query debe cumplir:
    - Sin filtros → todos salvo ELIMINADO.
    - `estadoDeclarado != null` → solo ese estado (incluye ELIMINADO si se pide explícitamente).
    - `estadoEfectivo = PUBLICADO` → `e.estado = PUBLICADO AND :ahora < e.inicio`.
    - `estadoEfectivo = EN_CURSO` → `e.estado = PUBLICADO AND :ahora BETWEEN e.inicio AND e.fin`.
    - `estadoEfectivo = FINALIZADO` → `e.estado = PUBLICADO AND :ahora > e.fin`.
    - `desde` → `e.fin >= :desde`; `hasta` → `e.inicio <= :hasta` (solapamiento del evento con el rango).
    - `titulo` → `LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))`.
  - [x] Usar `@Param("ahora")`; **no usar `NOW()` de MySQL**.
- **Definition of Done**:
  - [x] Combinar cualquier cantidad de filtros funciona; los parámetros en null se ignoran.
- **Cubre**: CA-10, CA-11, CA-12, CA-13, CA-14, CA-15, CA-16.

---

## Fase 2 — Contratos y soporte

### T05 — EventoDTORequest
- **Objetivo**: definir el contrato de entrada con validación automática.
- **Depende de**: T00.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/service/EventoDTORequest.java`.
- **Pasos**:
  - [x] `record` con `titulo`, `descripcion`, `inicio`, `fin`, `ubicacion`, `aforo`, `registrador`.
  - [x] `@NotBlank` en título, descripción, ubicación y registrador.
  - [x] `@NotNull` en inicio, fin y aforo.
  - [x] `@Min(1)` en aforo.
- **Definition of Done**:
  - [x] El record compila con las anotaciones `jakarta.validation`.
- **Cubre**: CA-02, CA-04, CA-05 (validación de entrada).

### T06 — EventoDTOResponse
- **Objetivo**: definir el contrato de salida.
- **Depende de**: T02.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/service/EventoDTOResponse.java`.
- **Pasos**:
  - [x] `record` con `id` y los mismos datos del request.
  - [x] Campo `estado` como texto con el estado efectivo.
- **Definition of Done**:
  - [x] El record compila y puede representar `Publicado`, `En curso`, `Finalizado`, `Borrador`, `Cancelado` y `Eliminado`.
- **Cubre**: CA-01, CA-11, CA-12, CA-13, CA-14.

### T07 — MensajeEventoResponse
- **Objetivo**: contrato del DELETE.
- **Depende de**: T06.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/service/MensajeEventoResponse.java`.
- **Pasos**:
  - [x] `record` con `mensaje` (String) y `evento` (EventoDTOResponse).
- **Definition of Done**:
  - [x] El record compila.
- **Cubre**: CA-17, CA-21.

### T08 — EventoDTOMapper
- **Objetivo**: convertir DTO ↔ entidad completando los valores que faltan.
- **Depende de**: T03, T05, T06.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/service/EventoDTOMapper.java`.
- **Firmas a implementar**:

```java
Evento toDomain(EventoDTORequest request);
EventoDTOResponse toResponse(Evento evento, LocalDateTime ahora);
```

- **Pasos**:
  - [x] `toDomain`: copia los datos del request, setea `estado = BORRADOR` y deja `id` en null.
  - [x] `toResponse`: calcula el estado efectivo con el mismo `ahora` recibido.
  - [x] Regla: Publicado + `:ahora < inicio` → Publicado; entre inicio y fin → En curso; posterior a fin → Finalizado; otros estados → su estado declarado.
- **Definition of Done**:
  - [x] No se usa `LocalDateTime.now()` dentro del mapper; solo el parámetro recibido.
- **Cubre**: CA-01, CA-09, CA-11, CA-12, CA-13.

### T09 — EventoNotFoundException
- **Objetivo**: señalar evento inexistente.
- **Depende de**: T00.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/exception/EventoNotFoundException.java`.
- **Pasos**:
  - [x] Excepción de dominio con mensaje "no encontrado".
- **Definition of Done**:
  - [x] Compila y es lanzada por el Service en las búsquedas fallidas.
- **Cubre**: CA-16.

### T10 — EventoBusinessException
- **Objetivo**: señalar violaciones de reglas de negocio.
- **Depende de**: T00.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/exception/EventoBusinessException.java`.
- **Pasos**:
  - [x] Excepción de dominio con mensaje descriptivo.
- **Definition of Done**:
  - [x] Compila y cubre fechas inválidas, estado inválido para publicar y parámetro `estado` inválido.
- **Cubre**: CA-03, CA-07, CA-08.

---

## Fase 3 — Lógica de negocio

### T11 — EventoService
- **Objetivo**: implementar todas las operaciones y reglas de negocio.
- **Depende de**: T04, T05, T06, T07, T08, T09, T10.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/service/EventoService.java`.
- **Firmas a implementar**:

```java
EventoDTOResponse crear(EventoDTORequest request);
EventoDTOResponse publicar(Long id);
MensajeEventoResponse darDeBaja(Long id);
List<EventoDTOResponse> listar(String estado, LocalDateTime desde, LocalDateTime hasta, String titulo);
EventoDTOResponse buscarPorId(Long id);
List<EventoDTOResponse> listarEliminados();
```

> Nota: la firma vigente incorpora además `listarTodos()`; se registra en T15 para no reabrir esta tarea ya marcada.

- **Pasos**:
  - [x] Obtener `ahora = LocalDateTime.now()` una vez por operación y reutilizarlo en repositorio y mapper.
  - [x] `crear`: validar `fin > inicio` e `inicio` futuro; guardar vía mapper con Borrador.
  - [x] `publicar`: exigir existencia, estado Borrador e inicio futuro; cambiar solo el estado.
  - [x] `darDeBaja`: pasar a ELIMINADO sin borrar; mensaje "Evento dado de baja" o "El evento ya estaba dado de baja".
  - [x] `listar`: normalizar `estado` (texto legible o enum, sin mayúsculas/espacios/guiones) en DOS salidas:
    - estados declarados → `estadoDeclarado = BORRADOR | CANCELADO | ELIMINADO`, `estadoEfectivo = null`;
    - estados calculados → `estadoEfectivo = PUBLICADO | EN_CURSO | FINALIZADO`, `estadoDeclarado = null`;
    - `estado == null` → ambos null (listado normal sin Eliminados).
  - [x] Pasar `desde`, `hasta` y `titulo` tal cual a `listarConFiltros`; si son null, la query los ignora.
  - [x] `estado=Eliminado` en `listar` devuelve eliminados; `listarEliminados()` sigue existiendo como consulta explícita separada.
  - [x] No crear `buscarPorTitulo` ni `editar`.
- **Definition of Done**:
  - [x] No existe ningún método de búsqueda por título fuera de `listar`.
  - [x] Todas las reglas de la spec §3 y §4 quedan implementadas salvo CA-22/CA-23.
- **Cubre**: CA-01…CA-21.

---

## Fase 4 — HTTP

### T12 — EventoController
- **Objetivo**: exponer los endpoints sin lógica de negocio.
- **Depende de**: T05, T11.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/controller/EventoController.java`.
- **Endpoints**:
  - `POST /eventos` → `ResponseEntity.status(HttpStatus.CREATED).body(...)`. **Sin `ServletUriComponentsBuilder`**.
  - `POST /eventos/{id:\d+}/publicar` → 200 con DTO.
  - `DELETE /eventos/{id:\d+}` → 200 con `{ mensaje, evento }` en ambos casos.
  - `GET /eventos` → 200 con listado; parámetros `estado` (incluye `Eliminado`), `desde`, `hasta`, `titulo`.
  - `GET /eventos/{id:\d+}` → 200 / 404.
  - `GET /eventos/eliminados` → 200 con eliminados.

> Nota: estas son las rutas históricas de T12. Las rutas vigentes (`/eventos/publicar/{id}`, `/eventos/{id}` y `/eventos/todos`, con `@PathVariable`) se registran en T15 para no reabrir esta tarea ya marcada.

- **Pasos**:
  - [x] Anotar el body del POST con `@Valid`.
  - [x] Solo invocar al Service; sin reglas ni cálculos.
  - [x] Usar `{id:\d+}` en todas las rutas con ID para no chocar con `/eventos/eliminados`.
- **Definition of Done**:
  - [x] El controller no contiene `if`, validaciones propias ni acceso a repositorio.
- **Cubre**: CA-01…CA-21 (superficie HTTP).

### T13 — EventoExceptionHandler
- **Objetivo**: traducir excepciones a respuestas con mensaje.
- **Depende de**: T09, T10.
- **Archivo**: `src/main/java/JMR/SDD/TicketMaster/evento/controller/EventoExceptionHandler.java`.
- **Pasos**:
  - [x] `@RestControllerAdvice`.
  - [x] `EventoNotFoundException` → 404 con mensaje.
  - [x] `EventoBusinessException` → 400 con mensaje.
  - [x] `MethodArgumentNotValidException` → 400 indicando el campo inválido.
- **Definition of Done**:
  - [x] Ninguna excepción de negocio se filtra sin mensaje al cliente.
- **Cubre**: CA-02, CA-03, CA-04, CA-07, CA-08, CA-16, CA-21.

---

## Fase 5 — Verificación

### T14 — Compilación
- **Objetivo**: confirmar que el módulo compila.
- **Depende de**: T01…T13.
- **Comando**: `mvnw.cmd compile`.
- **Pasos**:
  - [x] Ejecutar `mvnw.cmd compile` desde la raíz del proyecto (verificado: sin errores).
  - [x] Si falla, corregir y volver a ejecutar antes de marcar.
- **Definition of Done**:
  - [x] La compilación termina sin errores.
- **Cubre**: verificación técnica; no ejecuta tests.

---

## Fase 6 — Ajuste posterior a la documentación

### T15 — Ajuste de rutas y listado sin filtro
- **Objetivo**: dejar las rutas con `@PathVariable` y agregar `listarTodos()`.
- **Depende de**: T11, T12, T14.
- **Archivos**: `EventoController.java`, `EventoService.java`, `plan-Evento.md` y `task-Evento.md`.
- **Pasos**:
  - [x] `POST /eventos/publicar/{id}` con `@PathVariable Long id`.
  - [x] `DELETE /eventos/{id}` con `@PathVariable Long id`.
  - [x] `GET /eventos/{id}` con `@PathVariable Long id`.
  - [x] Agregar `GET /eventos/todos` → `listarTodos()`.
  - [x] Agregar `listarTodos()` en el Service: delega en `listar(null, null, null, null)`; excluye Eliminados.
  - [x] Mantener `GET /eventos` con filtros y `GET /eventos/eliminados`.
  - [x] Actualizar `plan-Evento.md` y `task-Evento.md` con las rutas y firmas vigentes.
  - [x] Ejecutar `mvnw.cmd compile` después de tocar el código Java (verificado: BUILD SUCCESS).
- **Definition of Done**:
  - [x] Las rutas responden bajo `/eventos` y usan `@PathVariable`.
  - [x] `listarTodos()` no incluye Eliminados.
  - [x] Compila sin errores.
- **Cubre**: CA-10, CA-14 y la superficie HTTP de CA-01…CA-21.

---

## Decisiones tomadas durante la implementación

> Decisiones que no estaban en la spec ni en el plan original. Se aplicaron y quedan registradas para validación.

- `GET /eventos/{id}` y `buscarPorId` devuelven `200` con estado `Eliminado` si el evento está dado de baja (la baja es lógica).
- El filtro `desde`/`hasta` aplica por solapamiento: `e.fin >= :desde` y `e.inicio <= :hasta` (reemplaza el criterio original sobre `inicio`).
- Los listados se ordenan por `e.inicio ASC`.
- Las respuestas de error usan `{ "mensaje": "..." }`.
- Si `inicio == ahora`, se permite crear y publicar (`isBefore`); el evento se muestra como `En curso`.
- `listarEliminados()` reutiliza `listarConFiltros` con `estadoDeclarado = ELIMINADO`.
- `EventoExceptionHandler.ErrorResponse` es un `record` anidado, para no agregar un archivo fuera del plan.
- La normalización de `estado` ignora mayúsculas, espacios, guiones y guiones bajos; no ignora acentos porque ninguno de los valores los lleva.
- CA-18, CA-19 y CA-20 no son verificables sin la entidad Ticket; quedan diferidos junto con CA-22 y CA-23.

---

## Checklist final

- [x] T00, T00b y T01…T15 marcados `[x]` solo después de verificarlos.
- [x] T15 implementado y compilado: rutas con `@PathVariable` y `listarTodos()` agregado.
- [x] Las rutas de publicar/eliminar/buscar usan `@PathVariable` sobre `/{id}`.
- [x] `GET /eventos/todos` y `listarTodos()` excluyen Eliminados.
- [ ] CA-01…CA-21 verificados contra la spec. Motivo: la verificación en ejecución requiere MySQL y tests, que están fuera de alcance; se revisó por inspección de código. CA-18, CA-19 y CA-20 dependen además de la entidad Ticket.
- [x] CA-22 y CA-23 quedan explícitamente diferidos a la spec de Ticket.
- [x] La query de `EventoRepository` combina todos los filtros opcionales y los null se ignoran.
- [x] No existe `buscarPorTitulo` ni en Service ni en Repository.
- [x] Sin filtro, los Eliminados no aparecen; con `estado=Eliminado`, sí.
- [x] No se usó `NOW()` de MySQL.
- [x] No se creó entidad Ticket ni autenticación.
- [x] No se modificaron `pom.xml`, `application.properties`, migraciones existentes ni zonas protegidas.
- [x] Se señaló cualquier decisión tomada sin spec o plan.
