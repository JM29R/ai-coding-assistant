# Tareas — Implementación del Módulo Respuestas (v1)

**Proyecto:** ForoHub · **Fecha:** 2026-11-17 · **Origen:** `spec-respuestas.md` (v1.2 Final) + `plan-respuestas.md` (v1), en esta misma carpeta.

**Instrucciones para la IA implementadora:** completar las tareas **en orden de dependencia**. Cada tarea debe durar máximo 20–30 minutos. Al terminar cada una, marcar su checkbox y reportar. **No escribir código fuera del alcance de la tarea actual.** Referencias: R# = regla de negocio de la spec (sección 4); plan = secciones de `plan-respuestas.md`. El módulo `topicos` ya está implementado; las tareas 13 y 14 lo editan.

## Tarea 1 — Migración Flyway V2
- **Cubre RFs:** infraestructura de datos para R1–R6 (ningún endpoint aún).
- **Depende de:** nada.
- **Descripción:** crear `V2__crear_tabla_respuestas.sql` según el plan (sección 2): 6 columnas en snake_case, FK `topico_id → topicos(id)` con `ON DELETE CASCADE`. No editar V1.
- **Hecho cuando:**
  - [x] existe la migración V2 con la tabla `respuestas` conforme al plan; la FK referencia `topicos(id)` con `ON DELETE CASCADE`. _(La aplicación de Flyway queda pendiente de verificar con una BD real; ver Tarea 16)._

## Tarea 2 — DTOs del módulo
- **Cubre RFs:** formato de entrada/salida de R1–R5.
- **Depende de:** nada.
- **Descripción:** crear los únicos dos DTOs del módulo (records) según el plan (sección 3): `RespuestaDTORequest` (mensaje, autor, topicoId) y `RespuestaDTOResponse` (id, mensaje, fechaCreacion, estado, autor, topicoId).
- **Hecho cuando:**
  - [x] ambos records existen en el package controller; el request no tiene id ni estado ni fecha; el response tiene los 6 campos; el proyecto compila.

## Tarea 3 — Entidad JPA y enum
- **Cubre RFs:** persistencia de R1–R6.
- **Depende de:** Tarea 1.
- **Descripción:** crear el enum `EstadoRespuesta` (ACTIVO/INACTIVO) y la entidad `Respuesta` mapeada a `respuestas`: id autogenerado (`GenerationType.IDENTITY`), `estado` como texto (`@Enumerated(EnumType.STRING)`), relación `@ManyToOne(fetch = LAZY) @JoinColumn(name = "topico_id")` con la entidad `Topico` del módulo `topicos`, Lombok (plan, sección 2).
- **Hecho cuando:**
  - [x] la entidad mapea las 6 columnas; el id es autogenerado; la relación con `Topico` es LAZY por `topico_id`; el proyecto compila.

## Tarea 4 — Repository
- **Cubre RFs:** consultas de R1 (topico activo), R5 (listado, detalle/actualizar/eliminar) y R6 (cascade).
- **Depende de:** Tarea 3.
- **Descripción:** crear `RespuestaRepository` (Spring Data JPA) con las 3 consultas derivadas del plan (sección 4): `findByIdAndEstado`, `findByTopicoIdAndEstadoOrderByFechaCreacionDesc`, `findByTopicoIdAndEstado`.
- **Hecho cuando:**
  - [x] el repositorio declara las 3 consultas con las firmas exactas del plan; ninguna usa SQL manual; el proyecto compila.

## Tarea 5 — Excepciones de dominio
- **Cubre RFs:** los errores 400/404 de R1–R4.
- **Depende de:** nada.
- **Descripción:** crear `ValidacionRespuestaException` (→ 400) y `RespuestaNoEncontradaException` (→ 404) con mensajes legibles en español (plan, sección 6). Nota: para topico faltante se reutiliza `TopicoNoEncontradoException` del módulo `topicos` (decisión 2026-11-17), no se crea una nueva.
- **Hecho cuando:**
  - [x] ambas excepciones existen en el package service con mensajes en español; no se duplicó la excepción de topico; el proyecto compila.

## Tarea 6 — Mapper
- **Cubre RFs:** conversión entrada→entidad de R1 y entidad→salida de R1–R5.
- **Depende de:** Tareas 2 y 3.
- **Descripción:** crear `RespuestaMapper` (@Component, package service) con `toDomain()` (entidad nueva sin id; `mensaje` y `autor` con `.trim()`; solo creación) y `toResponse()` (los 6 campos, incluido `topicoId` desde `respuesta.getTopico().getId()`). El topico, estado y fecha se completan en el service (plan, sección 5).
- **Hecho cuando:**
  - [x] el mapper es un componente con exactamente dos métodos; `toDomain` no asigna id, topico, estado ni fecha; `toResponse` incluye `topicoId`; el proyecto compila.

## Tarea 7 — Service: crear (R1, R2)
- **Cubre RFs:** POST; reglas R1 y R2.
- **Depende de:** Tareas 4, 5 y 6.
- **Descripción:** crear `RespuestasService` (`@Service @Transactional`, inyección por constructor de `RespuestaRepository`, `TopicoRepository` y `RespuestaMapper`) e implementar `crear` según el plan (sección 6): validar `mensaje`/`autor` no nulos ni `.isBlank()` y `topicoId` no nulo → 400; buscar topico activo con `topicoRepository.findByIdAndEstado(topicoId, ACTIVO)` → si no hay, `TopicoNoEncontradoException` (404); `toDomain()`; asignar topico, `estado = ACTIVO` y `fechaCreacion = ahora`; `save()`; `toResponse()`.
- **Hecho cuando:**
  - [x] `crear` rechaza mensaje/autor vacíos o solo espacios y topicoId nulo (400); topico inexistente o INACTIVO → 404; toda respuesta nace ACTIVO con fecha de ahora; el proyecto compila.

## Tarea 8 — Service: listar por topico y detalle (R5)
- **Cubre RFs:** GET listado y GET detalle; regla R5.
- **Depende de:** Tarea 7.
- **Descripción:** implementar `listarPorTopico(topicoId)` (verificar topico activo → 404 si no; `findByTopicoIdAndEstadoOrderByFechaCreacionDesc(topicoId, ACTIVO)`; mapear todo con `toResponse`; array completo sin paginar; topico activo sin respuestas → lista vacía) y `obtenerPorId(id)` (`findByIdAndEstado(id, ACTIVO)` → si no hay, `RespuestaNoEncontradaException` 404), según el plan (sección 6).
- **Hecho cuando:**
  - [x] `listarPorTopico` devuelve solo respuestas ACTIVO, más recientes primero, y `[]` para un topico activo sin respuestas; topico inexistente/inactivo → 404; `obtenerPorId` responde 404 para respuesta inexistente o inactiva; el proyecto compila.

## Tarea 9 — Service: actualizar (R3)
- **Cubre RFs:** PUT parcial; regla R3.
- **Depende de:** Tarea 8.
- **Descripción:** implementar `actualizar(id, request)` (PUT parcial) según el plan (sección 6): buscar respuesta activa por id → 404; si `request` es **nulo** o `request.mensaje()` llega con valor (`!= null && !isBlank()`) aplicar `setMensaje(trim)` y `save()` sobre la entidad cargada (JPA actualiza la fila porque conserva su id); si `request` es nulo, o `mensaje` llega `null`/`""`/solo espacios → 200 sin cambios; **ignorar siempre** `autor` y `topicoId`; sin verificación de duplicados; `toResponse()` final.
- **Hecho cuando:**
  - [x] `actualizar` acepta `request == null` (body ausente) sin lanzar error; modifica solo el mensaje cuando llega con valor; mensaje `null`/`""`/solo espacios → 200 sin cambios; id, autor, fecha, estado y topico nunca cambian; respuesta inexistente/inactiva → 404; el proyecto compila.

## Tarea 10 — Service: eliminar (R4)
- **Cubre RFs:** DELETE; regla R4.
- **Depende de:** Tarea 9.
- **Descripción:** implementar `eliminar(id)`: buscar respuesta activa por id → 404 si no existe o ya está inactiva; cambiar estado a INACTIVO; `save()` (borrado lógico, la fila permanece).
- **Hecho cuando:**
  - [x] `eliminar` deja la fila en la BD con estado INACTIVO; no borra físicamente; id inexistente o ya inactivo → 404; el proyecto compila.

## Tarea 11 — Service: inactivarPorTopico (R6)
- **Cubre RFs:** cascade lógico del borrado de topicos.
- **Depende de:** Tarea 4 (método `findByTopicoIdAndEstado`); coherentemente Tarea 10.
- **Descripción:** implementar `public void inactivarPorTopico(Long topicoId)` en `RespuestasService` (plan, sección 6): `findByTopicoIdAndEstado(topicoId, ACTIVO)` → a cada respuesta `setEstado(INACTIVO)` → `saveAll()`. Sin 404: si no hay respuestas activas, termina sin hacer nada.
- **Hecho cuando:**
  - [x] el método existe, es público y transaccional; marca INACTIVAS solo las respuestas activas del topico; no lanza error con cero respuestas; el proyecto compila.

## Tarea 12 — Controller
- **Cubre RFs:** exposición HTTP de R1–R4.
- **Depende de:** Tareas 2 y 7–10.
- **Descripción:** crear `RespuestasController` (`@RestController @RequestMapping("/respuestas")`) con los 5 endpoints y códigos de la spec (sección 5): POST `/respuestas` → 201 + `Location: /respuestas/{id}` + body; GET `/respuestas/topico/{topicoId}` → 200 array; GET `/respuestas/{id}` → 200; PUT → 200; DELETE → 204 (`ResponseEntity.noContent().build()`). El método del PUT declara `@PutMapping("/{id}")` con `@PathVariable Long id` y `@RequestBody(required = false) RespuestaDTORequest request`, de modo que un PUT con body `{}` o **sin body** no falle en el binding: `request` llega nulo o con campos nulos y el service responde 200 sin cambios (R3). Solo los dos DTOs; sin lógica ni referencias a la entidad (plan, sección 7).
- **Hecho cuando:**
  - [x] existen los 5 endpoints con los códigos HTTP exactos de la spec; POST incluye `Location`; el PUT recibe el body como opcional (`required = false`) y un body `{}` o ausente responde 200 sin cambios; el controller no importa ni referencia la entidad `Respuesta`; el proyecto compila.

## Tarea 13 — Integración: handlers en el advice central
- **Cubre RFs:** formato de error de R1–R4 (400/404 con `{"mensaje": "..."}`).
- **Depende de:** Tarea 5 (y coherentemente Tarea 12).
- **Descripción:** editar `ManejadorExcepciones` del módulo `topicos` (plan, sección 8): agregar handlers para `ValidacionRespuestaException` → 400 y `RespuestaNoEncontradaException` → 404, reutilizando el record `Error` existente. `TopicoNoEncontradoException` ya está mapeada y sirve tal cual para R1.
- **Hecho cuando:**
  - [x] el advice central traduce las dos excepciones nuevas a `{"mensaje": "..."}` con 400/404; sin stack traces; el proyecto compila.

## Tarea 14 — Integración: cascade lógico en TopicosService
- **Cubre RFs:** R6 (borrar un topico inactiva sus respuestas activas en la misma transacción).
- **Depende de:** Tareas 11 y 13.
- **Descripción:** editar `TopicosService.eliminar(Long id)` (plan, sección 9): inyectar `RespuestasService` por constructor y, tras marcar el topico INACTIVO, invocar `respuestasService.inactivarPorTopico(id)` (la transacción de clase ya cubre ambos saves). No hay ciclo de beans.
- **Hecho cuando:**
  - [x] `eliminar` de un topico deja INACTIVAS todas sus respuestas activas; `TopicosService` referencia solo al service de respuestas (no a su repository); el proyecto compila.

## Tarea 15 — Verificación de compilación
- **Cubre RFs:** requisito transversal (criterio final de la spec, sección 9).
- **Depende de:** Tareas 1–14.
- **Descripción:** compilar el proyecto completo y resolver cualquier error; es la verificación obligatoria del proyecto (AGENTS.md).
- **Hecho cuando:**
  - [x] `mvnw.cmd compile` termina sin errores.

## Tarea 16 — Verificación funcional (criterios de aceptación)
- **Cubre RFs:** R1–R6 completos.
- **Depende de:** Tarea 15.
- **Descripción:** arrancar la app con las variables de BD (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), dejar que Flyway aplique V2, y probar manualmente con un cliente REST los criterios de aceptación de la spec (sección 9), incluyendo casos límite: crear sobre topico inexistente/inactivo → 404; listar de topico inexistente/inactivo → 404; topico activo sin respuestas → 200 con `[]`; respuesta inactiva → 404 en GET/PUT/DELETE; PUT con mensaje solo espacios → 200 sin cambios; PUT con body `{}` o sin body → 200 sin cambios; PUT ignorando autor/topicoId aunque lleguen; DELETE de respuesta dos veces → segunda vez 404; DELETE de topico → sus respuestas quedan INACTIVAS (R6); `topicoId` no nulo en el body de creación.
- **Hecho cuando:**
  - [ ] los 9 criterios de aceptación de la spec (sección 9) quedan verificados y marcados, más los casos límite; Flyway aplicó la migración V2 sin errores; se entregó al humano el resumen con la checklist final.
