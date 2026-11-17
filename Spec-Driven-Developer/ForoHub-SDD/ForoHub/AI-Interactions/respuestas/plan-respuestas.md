# Plan — Implementación del Módulo Respuestas (v1)

**Proyecto:** ForoHub · **Fecha:** 2026-11-17 · **Origen:** `spec-respuestas.md` (v1.2 Final), en esta misma carpeta · **Estado:** Aprobado para implementación

**A quién va dirigido:** a la IA que implementará el módulo. Este plan contiene el **cómo** (estructura, modelos, JSON y detalles técnicos). La especificación (`spec-respuestas.md`) conserva el **qué** y el **porqué** (requisitos funcionales, reglas de negocio, casos límite). Leer ambos documentos antes de escribir código. No se debe escribir código hasta que el humano lo ordene; este documento es la guía técnica de esa implementación.

**Contexto de código real:** el módulo `topicos` ya está implementado (`TopicosService`, `ManejadorExcepciones`, `V1__crear_tabla_topicos.sql` aplicada). Este plan espeja sus patrones y **incluye dos ediciones** en ese módulo (sección 9).

## 1. Estructura del módulo

```
src/main/java/JMR/SDD/ForoHub/respuestas/
├── controller/
│   ├── RespuestasController.java           // 5 endpoints REST, sin lógica
│   ├── RespuestaDTORequest.java            // record: mensaje, autor, topicoId
│   └── RespuestaDTOResponse.java           // record: id, mensaje, fechaCreacion, estado, autor, topicoId
├── entity/
│   ├── Respuesta.java                      // entidad JPA, tabla respuestas (@ManyToOne con Topico)
│   ├── EstadoRespuesta.java                // enum ACTIVO / INACTIVO
│   └── RespuestaRepository.java            // Spring Data JPA + consultas derivadas
└── service/
    ├── RespuestasService.java              // reglas R1–R6 + validaciones + inactivarPorTopico()
    ├── RespuestaMapper.java                // @Component: toDomain() / toResponse()
    ├── ValidacionRespuestaException.java   // → HTTP 400
    └── RespuestaNoEncontradaException.java // → HTTP 404 (solo para respuestas)
```

Únicamente **dos DTOs** en el módulo (`RespuestaDTORequest` y `RespuestaDTOResponse`); la entidad JPA nunca sale del service y no aparece en el controller. El advice compartido y `TopicoNoEncontradoException` viven en el módulo `topicos` (decisión 2026-11-17: reutilizar, no duplicar).

## 2. Modelo de datos técnico (tabla `respuestas`)

| Columna | Tipo MySQL | Detalle |
|---|---|---|
| id | BIGINT AUTO_INCREMENT, PK | id autogenerado por JPA (`@GeneratedValue(strategy = GenerationType.IDENTITY)`) |
| mensaje | TEXT NOT NULL | obligatorio, sin longitud máxima |
| fecha_creacion | DATETIME NOT NULL | sin precisión extra, idéntico a `topicos` (decisión 2026-11-17); solo se establece al crear; no actualizable |
| estado | VARCHAR(20) NOT NULL | enum persistido como texto: `ACTIVO` / `INACTIVO` |
| autor | VARCHAR(100) NOT NULL | texto libre, obligatorio |
| topico_id | BIGINT NOT NULL | FK → `topicos(id)` con `ON DELETE CASCADE` |

- Migración: `V2__crear_tabla_respuestas.sql` en `src/main/resources/db/migration/` (asume `V1__crear_tabla_topicos.sql` ya aplicada; restricción del proyecto: nunca editar una migración ya aplicada).
- Entidad JPA: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "topico_id") private Topico topico;`; `estado` con `@Enumerated(EnumType.STRING)`; Lombok para accesores (regla de AGENTS.md).

## 3. DTOs y JSON

- **RespuestaDTORequest** (sirve para crear y actualizar): `mensaje`, `autor`, `topicoId`. Sin id: lo genera la base de datos. En el PUT solo se usa `mensaje`; `autor` y `topicoId` se **ignoran** (decisión 2026-11-17).
- **RespuestaDTOResponse** (toda salida del backend): `id`, `mensaje`, `fechaCreacion`, `estado`, `autor`, `topicoId` (tomado de `respuesta.getTopico().getId()`; LAZY es seguro porque el mapeo ocurre dentro de la transacción del service).
- `fechaCreacion` se serializa en formato ISO-8601 (comportamiento por defecto del serializador JSON del stack web); `estado` se serializa como texto del enum.
- Listado: array JSON directo de `RespuestaDTOResponse` (sin wrapper de página).
- Errores: objeto `{"mensaje": "..."}` con texto legible en español, sin stack traces.

## 4. Repository (consultas derivadas, sin SQL manual)

- `Optional<Respuesta> findByIdAndEstado(Long id, EstadoRespuesta estado)` — detalle/actualizar/eliminar.
- `List<Respuesta> findByTopicoIdAndEstadoOrderByFechaCreacionDesc(Long topicoId, EstadoRespuesta estado)` — listado del topico, más recientes primero.
- `List<Respuesta> findByTopicoIdAndEstado(Long topicoId, EstadoRespuesta estado)` — cascade lógico (R6).

## 5. Mapper (`RespuestaMapper`, @Component en el package service)

- `toDomain(RespuestaDTORequest)` → entidad `Respuesta` nueva, **sin id**, con `mensaje` y `autor` normalizados con `.trim()`. Solo para creación; el service asigna `topico`, `estado = ACTIVO` y `fechaCreacion = ahora` (R1 es lógica de negocio, no del mapper).
- `toResponse(Respuesta)` → `RespuestaDTOResponse` con los 6 campos de la entidad.

## 6. Service (`RespuestasService`) — reglas → técnica

`@Service @Transactional` a nivel de clase (igual que `TopicosService`); las consultas de lectura marcadas como solo lectura. Inyección por constructor: `RespuestaRepository`, `TopicoRepository` (del módulo `topicos`) y `RespuestaMapper`.

- **crear (R1–R2):** validar `mensaje` y `autor` no nulos ni `.isBlank()` y `topicoId` no nulo → 400 (`ValidacionRespuestaException`); buscar topico activo con `topicoRepository.findByIdAndEstado(topicoId, EstadoTopico.ACTIVO)` → si no hay, `TopicoNoEncontradoException` (404, **decisión 2026-11-17: reutilizada del módulo topicos**); `toDomain()`; completar topico, estado y fecha; `save()`; `toResponse()`.
- **listarPorTopico (R5):** verificar topico activo igual que arriba → 404 si no; `findByTopicoIdAndEstadoOrderByFechaCreacionDesc(topicoId, ACTIVO)`; mapear toda la lista con `toResponse()`; devolver array completo. Topico activo sin respuestas → 200 con `[]`.
- **obtenerPorId (R5):** `respuestaRepository.findByIdAndEstado(id, ACTIVO)` → si no hay, `RespuestaNoEncontradaException` (404); `toResponse()`.
- **actualizar (R3, PUT parcial):** 1) buscar respuesta activa por id → 404 si no existe o está inactiva; 2) si `request.mensaje()` tiene valor (`!= null && !isBlank()`), aplicar `setMensaje(trim)` y `save()`; si llega `null`, `""` o solo espacios, conservar el valor de la BD y devolver 200 sin cambios; 3) `autor` y `topicoId` se ignoran siempre; 4) `toResponse()` sobre la entidad actualizada. Sin verificación de duplicados (decisión de spec).
- **eliminar (R4):** buscar activa → 404 si no hay; cambiar estado a `INACTIVO`; `save()`.
- **inactivarPorTopico (R6):** `public void inactivarPorTopico(Long topicoId)`: `findByTopicoIdAndEstado(topicoId, ACTIVO)` → a cada respuesta `setEstado(INACTIVO)` → `saveAll()`. Sin 404: si no hay respuestas activas, termina sin hacer nada. Lo invoca `TopicosService.eliminar()`; ambos services son `@Transactional`, la transacción se propaga (REQUIRED).
- Helpers privados `validarObligatorio` / `tieneValor`, mismo estilo que en topicos. Excepciones de dominio (`ValidacionRespuestaException` 400, `RespuestaNoEncontradaException` 404) con mensajes en español; el advice las convierte en los errores JSON del contrato.

## 7. Controller (`RespuestasController`)

- `@RestController @RequestMapping("/respuestas")`. Endpoints: POST `/respuestas` → 201 + cabecera `Location: /respuestas/{id}` + body; GET `/respuestas/topico/{topicoId}` → 200 `List`; GET `/respuestas/{id}` → 200; PUT `/respuestas/{id}` → 200; DELETE `/respuestas/{id}` → 204 (`ResponseEntity.noContent().build()`).
- Spring prioriza el literal `topico/{topicoId}` sobre el patrón `/{id}`: no hay conflicto de mapping.
- Recibe y devuelve solo los dos DTOs; cero lógica de negocio y cero referencias a la entidad.

## 8. Manejador de errores (compartido, en módulo `topicos`)

- En `ManejadorExcepciones` (Topicos.controller) se **agregan dos handlers**: `ValidacionRespuestaException` → 400 y `RespuestaNoEncontradaException` → 404, cuerpo `{"mensaje": "..."}` reutilizando el record `Error` existente.
- `TopicoNoEncontradoException` ya está mapeada a 404: sirve tal cual para el caso R1 de topico faltante o inactivo.

## 9. Cambios en el módulo `topicos` (integración, mismo plan)

1. **`TopicosService.eliminar(Long id)`:** se inyecta `RespuestasService` (constructor) y, dentro del método, tras marcar el topico `INACTIVO`, se invoca `respuestasService.inactivarPorTopico(id)` (cascade lógico R6; la transacción de clase ya cubre ambos saves).
2. **`ManejadorExcepciones`:** los dos handlers nuevos de la sección 8.

No hay ciclo de beans: `RespuestasService` → `TopicoRepository` (no inyecta `TopicosService`); `TopicosService` → `RespuestasService`.

## 10. Orden de implementación

1. Migración SQL `V2__crear_tabla_respuestas.sql`
2. Entity: `EstadoRespuesta`, `Respuesta`
3. `RespuestaRepository`
4. `RespuestaMapper`
5. Excepciones de dominio
6. `RespuestasService`
7. DTOs (los dos records)
8. `RespuestasController`
9. Integración en `topicos`: handlers en `ManejadorExcepciones` + `inactivarPorTopico` en `TopicosService.eliminar()`
10. Verificación: `mvnw.cmd compile` → arrancar con variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (Flyway aplica V2) → probar manualmente los criterios de aceptación de la spec (sección 9).

## 11. Notas y riesgos técnicos

- **Dependencia cruzada de packages** (`respuestas` ↔ `topicos`): aceptada por decisión del humano (advice y excepción compartidos, cascade R6). Sin ciclo de beans. Si a futuro molesta, se evalúa mover lo compartido a un package común.
- **DATETIME sin microsegundos:** dos respuestas creadas en el mismo segundo quedan con fecha igual y su orden relativo es indeterminado; riesgo aceptado por decisión del humano (consistencia con `topicos`). No se puede editar V1.
- Listado sin paginar: coste de escalabilidad aceptado por decisión del humano; si el volumen crece, requiere revisión de la spec.
- Stack: Java 21, Spring Boot 4.0.9 con starter webmvc; validación manual (sin starter de validación, para no tocar el `pom.xml`).
- Sin tests automatizados en esta versión (fuera de alcance declarado en la spec v1.2).
- Convención del proyecto: código y comentarios en español; packages en minúscula; compilar con `mvnw.cmd` es obligatorio al terminar.
