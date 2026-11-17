# Especificación — Módulo Respuestas (v1.2 Final)

**Proyecto:** ForoHub · **Fecha:** 2026-11-17 · **Estado:** Aprobada

## 1. Resumen
Módulo independiente que gestiona el ciclo de vida de las **Respuestas** del foro: creación, consulta, actualización y eliminación lógica. Una Respuesta es un aporte o contestación asociada a un Tópico (`@ManyToOne`). Módulo independiente con la misma arquitectura en capas (`controller`, `service`, `entity`) y convenciones que el módulo `topicos`.

## 2. Alcance
**Incluye:**
- CRUD completo de Respuestas bajo la ruta base única `/respuestas`.
- Borrado lógico con enum de estado (`ACTIVO` / `INACTIVO`).
- Listado de respuestas activas de un tópico mediante `GET /respuestas/topico/{topicoId}` (sin paginar).
- Cascade lógico al borrar un tópico: implementación de `inactivarPorTopico(topicoId)` en `RespuestasService` e invocación desde `TopicosService`.
- Migración Flyway `V2__crear_tabla_respuestas.sql`.

**No incluye:**
- Nuevos endpoints en `TopicosController`, usuarios/autenticación, tests automatizados nuevos, seguridad, paginación, regla anti-duplicados ni reactivación de respuestas inactivas.

## 3. Modelo de datos (tabla `respuestas`)

| Campo (Java) | Columna (MySQL) | Tipo SQL / Java | Restricciones |
|---|---|---|---|
| `id` | `id` | `BIGINT AUTO_INCREMENT` / `Long` | PK, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `mensaje` | `mensaje` | `TEXT` / `String` | `NOT NULL` |
| `fechaCreacion` | `fecha_creacion` | `DATETIME` / `LocalDateTime` | `NOT NULL`, asignada al crear; no editable |
| `estado` | `estado` | `VARCHAR(20)` / `EstadoRespuesta` | `NOT NULL`, valores: `ACTIVO`, `INACTIVO` (default `ACTIVO`) |
| `autor` | `autor` | `VARCHAR(100)` / `String` | `NOT NULL`, texto libre (sin módulo de usuarios) |
| `topico` | `topico_id` | `BIGINT` / `Topico` (`@ManyToOne`) | `NOT NULL`, FK → `topicos(id)` con `ON DELETE CASCADE` |

**Reglas de la tabla (MySQL, `V2__crear_tabla_respuestas.sql`):**
- Columnas en `snake_case`: `id`, `mensaje`, `fecha_creacion`, `estado`, `autor`, `topico_id`.
- `estado` se guarda como texto (`@Enumerated(EnumType.STRING)`).
- `topico_id` debe coincidir en tipo (`BIGINT`) con `topicos.id` y tener **Foreign Key a `topicos(id)` con `ON DELETE CASCADE`** (respaldo físico ante borrados directos en BD; el borrado de aplicación es lógico, ver R4 y R6).
- `fechaCreacion` no actualizable; se establece solo en la creación.

## 4. Reglas de negocio
1. **R1 — Creación:** toda respuesta nace con estado `ACTIVO` y `fechaCreacion` = ahora (`LocalDateTime.now()`). El `topicoId` llega en `RespuestaDTORequest`; `RespuestasService` busca el tópico en `TopicoRepository` y verifica que exista y esté `ACTIVO`; si no existe o está `INACTIVO` → lanza `RespuestaNoEncontradaException` (404).
2. **R2 — Validación de entrada:** `mensaje` y `autor` son obligatorios al crear (no nulos ni en blanco, validado con `.isBlank()`) y `topicoId` no nulo → lanza `ValidacionRespuestaException` (400). Al persistir se aplica `.trim()` a `mensaje` y `autor`. Sin longitud máxima y **sin regla anti-duplicados**.
3. **R3 — Actualización parcial:** `PUT /respuestas/{id}` actualiza **solo el `mensaje`**, y únicamente si llega con texto válido (`mensaje != null && !mensaje.isBlank()`, aplicando `.trim()`); si llega `null`, `""` o solo espacios, se conserva el valor existente en la base de datos. `id`, `autor`, `fechaCreacion`, `estado` y `topicoId` nunca se editan por esta vía. Body sin mensaje modificable → `200` con la respuesta sin cambios.
4. **R4 — Borrado lógico:** `DELETE /respuestas/{id}` no elimina la fila; cambia `estado` a `INACTIVO`. Si la respuesta no existe o ya está `INACTIVA` → 404.
5. **R5 — Visibilidad:** el listado y el detalle solo muestran respuestas `ACTIVO`; una respuesta inactiva se comporta como inexistente para las consultas (`404` en detalle/actualización/borrado). Listar respuestas de un tópico inexistente o `INACTIVO` → `404`.
6. **R6 — Cascade lógico:** cuando `DELETE /topicos/{id}` borra lógicamente un tópico en `TopicosService`, en la misma transacción (`@Transactional`) se marcan `INACTIVAS` todas sus respuestas `ACTIVAS`. Lo implementa el módulo Respuestas exponiendo `inactivarPorTopico(topicoId)` en `RespuestasService`, invocado por `TopicosService` (acoplamiento service → service, no entidad → entidad).

## 5. Contrato de API (ruta base única: `/respuestas`)

*Todas las rutas viven bajo `/respuestas` para evitar conflictos de mapping con `TopicosController`.*

| Método | Ruta | Éxito | Error | Descripción |
|---|---|---|---|---|
| `POST` | `/respuestas` | `201` + `RespuestaDTOResponse` + `Location` (`/respuestas/{id}`) | `400` / `404` | Crear respuesta asociada a un `topicoId` |
| `GET` | `/respuestas/topico/{topicoId}` | `200` + `List<RespuestaDTOResponse>` | `404` | Listar respuestas activas del tópico |
| `GET` | `/respuestas/{id}` | `200` + `RespuestaDTOResponse` | `404` | Detalle de una respuesta activa |
| `PUT` | `/respuestas/{id}` | `200` + `RespuestaDTOResponse` | `400` (JSON inválido) / `404` | Actualizar mensaje parcialmente |
| `DELETE` | `/respuestas/{id}` | `204` (`ResponseEntity.noContent().build()`) | `404` | Borrado lógico |

**DTO de entrada (`RespuestaDTORequest`):**
```java
public record RespuestaDTORequest(
    String mensaje,
    String autor,
    Long topicoId
) {}
```

**DTO de salida (`RespuestaDTOResponse`):**
```java
public record RespuestaDTOResponse(
    Long id,
    String mensaje,
    LocalDateTime fechaCreacion,
    EstadoRespuesta estado,
    String autor,
    Long topicoId
) {}
```

**Request de creación (`POST /respuestas`):**
```json
{
  "mensaje": "Mirá la documentación de @OneToMany",
  "autor": "JMR",
  "topicoId": 5
}
```

**Request de actualización (`PUT /respuestas/{id}` — parcial; solo el mensaje es editable):**
```json
{
  "mensaje": "Texto corregido"
}
```

**Response (`200` / `201`):**
```json
{
  "id": 1,
  "mensaje": "Mirá la documentación de @OneToMany",
  "fechaCreacion": "2026-11-17T10:30:00",
  "estado": "ACTIVO",
  "autor": "JMR",
  "topicoId": 5
}
```

**Listado (`GET /respuestas/topico/{topicoId}`):**
- Array JSON directo (`List<RespuestaDTOResponse>`, sin paginar), orden `fechaCreacion` **descendente** (más recientes primero), solo respuestas `ACTIVO`, vía método del repository `findByTopicoIdAndEstadoOrderByFechaCreacionDesc(Long topicoId, EstadoRespuesta estado)` pasando `EstadoRespuesta.ACTIVO`.
- Advertencia aceptada: sin paginar, este endpoint escala mal con el crecimiento de datos.

**Formato de error (`400` / `404`):**
- Cuerpo JSON `{"mensaje": "..."}` con texto legible en español, generado por el `@RestControllerAdvice` central del proyecto; sin stack traces.

## 6. Validaciones de entrada (decisión v1: validación manual en el service, sin Bean Validation)
- Al crear (`POST /respuestas`): `mensaje` y `autor` obligatorios (no nulos ni `.isBlank()`) y `topicoId` no nulo → `400` (`ValidacionRespuestaException`). Se aplica `.trim()` antes de guardar.
- Al actualizar (`PUT /respuestas/{id}`, R3): un mensaje `null`, `""` o solo espacios no es error; se conserva el valor existente en la base de datos y retorna `200`.
- Tópico inexistente o `INACTIVO` al crear o listar → `404` (`RespuestaNoEncontradaException`).
- Respuesta inexistente o `INACTIVA` en detalle/actualizar/eliminar → `404` (`RespuestaNoEncontradaException`).

## 7. Arquitectura del módulo (package `JMR.SDD.ForoHub.respuestas`)
Misma estructura que `topicos`: carpetas `controller`, `entity` y `service`. La entidad JPA nunca se expone en el controller.

```
FRONTEND → JSON → RespuestasController(@RequestBody RespuestaDTORequest)
  → RespuestasService (reglas R1–R6, validaciones manuales)
  → TopicoRepository (verifica tópico existente y ACTIVO)
  → RespuestaMapper.toDomain(request) → Respuesta (sin id)
  → Service completa topico, estado = ACTIVO, fechaCreacion = now()
  → RespuestaRepository.save(respuesta) → Respuesta (con id autogenerado)
  → RespuestaMapper.toResponse(respuesta) → RespuestaDTOResponse
  → RespuestasController retorna ResponseEntity<RespuestaDTOResponse> → JSON → FRONTEND
```

- **`entity`**:
    - Entidad JPA `Respuesta`: `id` (`Long`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`), `mensaje`, `fechaCreacion`, `estado` (`@Enumerated(EnumType.STRING)`), `autor`, relación `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "topico_id") Topico topico`.
    - Enum `EstadoRespuesta` (`ACTIVO`, `INACTIVO`).
    - `RespuestaRepository` (`extends JpaRepository<Respuesta, Long>`):
        - `Optional<Respuesta> findByIdAndEstado(Long id, EstadoRespuesta estado)`
        - `List<Respuesta> findByTopicoIdAndEstadoOrderByFechaCreacionDesc(Long topicoId, EstadoRespuesta estado)`
        - `List<Respuesta> findByTopicoIdAndEstado(Long topicoId, EstadoRespuesta estado)` (usado en R6).
- **`service`**:
    - `RespuestasService`: implementa reglas R1–R6, validaciones manuales y el método `@Transactional public void inactivarPorTopico(Long topicoId)` para el cascade lógico (R6), invocado desde `TopicosService`.
    - `RespuestaMapper` (`@Component`):
        - `public Respuesta toDomain(RespuestaDTORequest request)` → construye entidad `Respuesta` nueva sin `id` mapeando `mensaje` y `autor` (se usa **exclusivamente en la creación**; `RespuestasService` asigna `topico`, `estado` y `fechaCreacion`).
        - `public RespuestaDTOResponse toResponse(Respuesta respuesta)` → mapea la entidad `Respuesta` a `RespuestaDTOResponse`.
    - **Flujo de actualización (`PUT`, R3):** el service busca primero la respuesta activa con `findByIdAndEstado(id, EstadoRespuesta.ACTIVO)`; si no existe o está inactiva, lanza `RespuestaNoEncontradaException` (`404`). Sobre esa misma entidad cargada actualiza `mensaje` (con `.trim()`) solo si llega con texto válido y llama a `save()`; como la entidad conserva su `id` autogenerado, JPA **actualiza la fila existente** en lugar de insertar una nueva. La salida se construye con `RespuestaMapper.toResponse(respuesta)`.
    - Excepciones propias del módulo: `RespuestaNoEncontradaException` (`404`) y `ValidacionRespuestaException` (`400`).
- **`controller`**:
    - `RespuestasController` (`@RestController`, `@RequestMapping("/respuestas")`): recibe `@RequestBody RespuestaDTORequest` y devuelve siempre `ResponseEntity<RespuestaDTOResponse>`, `ResponseEntity<List<RespuestaDTOResponse>>` o `ResponseEntity<Void>`. No contiene lógica de negocio ni referencias a la entidad `Respuesta`.
    - **Manejo centralizado de excepciones:** se agregan los handlers de `RespuestaNoEncontradaException` (`404`) y `ValidacionRespuestaException` (`400`) en el `@RestControllerAdvice` existente en el módulo `topicos` (junto a `TopicoNoEncontradoException` y `ValidacionTopicoException`) para mantener un único manejador global y formato de error uniforme `{"mensaje": "..."}`.
- **Integración en `TopicosService` (módulo `topicos`):**
    - Se inyecta `RespuestasService` en `TopicosService` y se invoca `respuestasService.inactivarPorTopico(id)` dentro del método de borrado lógico de tópico (anotado con `@Transactional`).

## 8. Migración Flyway
- Archivo `V2__crear_tabla_respuestas.sql` en `src/main/resources/db/migration/`, creando la tabla `respuestas` según la sección 3 (asume que `V1__crear_tabla_topicos.sql` ya está aplicada).
- Restricción de proyecto: nunca editar una migración ya aplicada.

## 9. Criterios de aceptación
- [ ] `POST /respuestas` recibe `@RequestBody RespuestaDTORequest`, crea la respuesta y devuelve `ResponseEntity` `201` con `Location: /respuestas/{id}` y `RespuestaDTOResponse`; `topicoId` inexistente o inactivo → `404`; `mensaje`, `autor` o `topicoId` nulos/en blanco → `400`.
- [ ] `GET /respuestas/topico/{topicoId}` devuelve `ResponseEntity<List<RespuestaDTOResponse>>` con las respuestas activas del tópico, más recientes primero; tópico inexistente o inactivo → `404`.
- [ ] `GET /respuestas/{id}` de respuesta activa → `200`; de inexistente o inactiva → `404`.
- [ ] `PUT /respuestas/{id}` recibe `@RequestBody RespuestaDTORequest`, actualiza solo el mensaje (con `.trim()`); body con mensaje `null`, `""` o solo espacios → `200` sin cambios; respuesta inexistente o inactiva → `404`.
- [ ] `DELETE /respuestas/{id}` devuelve `204` y la respuesta pasa a `INACTIVO`; id inexistente o ya inactiva → `404`.
- [ ] `DELETE /topicos/{id}` deja `INACTIVAS` todas las respuestas activas del tópico (R6, cascade lógico).
- [ ] Errores `400`/`404` con cuerpo `{"mensaje": "..."}` desde el `@RestControllerAdvice` centralizado.
- [ ] Migración Flyway `V2__crear_tabla_respuestas.sql` aplicada sin errores al arrancar la app (requiere variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- [ ] Proyecto compila sin errores con `mvnw.cmd compile`.

## 10. Decisiones registradas en esta versión (2026-11-17)
1. **Módulo independiente:** Respuestas tiene la misma estructura de carpetas que Topicos (`controller`, `service`, `entity`) sin acoplamiento de rutas.
2. **Ruta base única `/respuestas`:** se evita usar `/topicos` en `RespuestasController` para no colisionar con `TopicosController`. El listado por tópico queda en `GET /respuestas/topico/{topicoId}` y la creación en `POST /respuestas` recibiendo `topicoId` en el body.
3. **DTOs y Mapper explícitos:** entrada vía `@RequestBody RespuestaDTORequest`, salida envuelta en `ResponseEntity<RespuestaDTOResponse>`, y transformación mediante `RespuestaMapper` (`toDomain` para crear y `toResponse` para responder).
4. **Borrado lógico y cascade transaccional (R6):** `TopicosService` inyecta `RespuestasService` e invoca `inactivarPorTopico(topicoId)` dentro de la misma transacción al dar de baja un tópico. Además, la FK en MySQL incluye `ON DELETE CASCADE` como respaldo físico.
5. **Actualización sobre entidad gestionada por JPA:** al tener `id` (`BIGINT AUTO_INCREMENT`) cargado desde BD, `save()` actualiza automáticamente la fila existente modificando solo `mensaje`.
6. **Validación con `.isBlank()` y `.trim()`:** rechaza textos compuestos solo por espacios en creación (`400`) y limpia espacios extremos al persistir.
7. **Manejador centralizado compartido:** se reutiliza el `@RestControllerAdvice` del módulo `topicos` agregando handlers para `RespuestaNoEncontradaException` y `ValidacionRespuestaException`.
8. **Sin tests nuevos en esta iteración:** primer flujo SDD del proyecto; se valida con compilación (`mvnw.cmd compile`) y pruebas manuales de endpoints.