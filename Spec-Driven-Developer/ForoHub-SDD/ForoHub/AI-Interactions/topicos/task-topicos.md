# Tareas — Implementación del Módulo Topicos (v1)

**Proyecto:** ForoHub · **Fecha:** 2026-11-13 · **Origen:** `spec-topicos.md` (v1.2) + `plan-topicos.md` (v1), en esta misma carpeta.

**Instrucciones para la IA implementadora:** completar las tareas **en orden de dependencia**. Cada tarea debe durar máximo 20–30 minutos. Al terminar cada una, marcar su checkbox y reportar. **No escribir código fuera del alcance de la tarea actual.** Referencias: RF = requisito funcional de la spec (sección 4); R# = regla de negocio (sección 6); plan = secciones de `plan-topicos.md`.

## Tarea 1 — Migración Flyway V1
- **Cubre RFs:** infraestructura de datos para RF1–RF5 (ningún endpoint aún).
- **Depende de:** nada.
- **Descripción:** crear la migración de la tabla `topicos` según el modelo de datos técnico del plan (sección 2): 6 columnas, snake_case, solo esta tabla.
- **Hecho cuando:**
  - [ ] existe `src/main/resources/db/migration/V1__crear_tabla_topicos.sql` con la tabla `topicos` y sus 6 columnas conforme al plan; al arrancar la app con las variables de BD configuradas, Flyway la aplica sin errores.

## Tarea 2 — DTOs del módulo
- **Cubre RFs:** el formato de entrada/salida de RF1–RF5.
- **Depende de:** nada.
- **Descripción:** crear los únicos dos DTOs del módulo (records) según el plan (sección 3): request con todo menos id; response con los 6 campos.
- **Hecho cuando:**
  - [ ] existen `TopicoDTORequest` y `TopicoDTOResponse` en el package controller; el request no tiene id; el response tiene id, titulo, mensaje, fechaCreacion, estado y autor; el proyecto compila.

## Tarea 3 — Entidad JPA y enum
- **Cubre RFs:** persistencia de RF1–RF5.
- **Depende de:** Tarea 1.
- **Descripción:** crear el enum `EstadoTopico` (ACTIVO/INACTIVO) y la entidad `Topico` mapeada a la tabla `topicos`: id autogenerado por JPA con la notación correspondiente, estado persistido como texto, `fechaCreacion` no actualizable, Lombok para accesores (plan, sección 2).
- **Hecho cuando:**
  - [ ] la entidad mapea las 6 columnas de la tabla; el id es autogenerado; el estado es el enum como texto; `fechaCreacion` está marcado como no actualizable; el proyecto compila.

## Tarea 4 — Repository
- **Cubre RFs:** consultas de RF2 (listado), RF3/RF4/RF5 (búsqueda activa por id) y regla R2 (duplicados).
- **Depende de:** Tarea 3.
- **Descripción:** crear `TopicoRepository` (Spring Data JPA) con las 4 consultas derivadas descritas en el plan (sección 4): activos por fecha descendente, activo por id, duplicados al crear y duplicados al actualizar excluyendo el propio id.
- **Hecho cuando:**
  - [ ] el repositorio declara las 4 consultas del plan; ninguna usa SQL manual; el proyecto compila.

## Tarea 5 — Excepciones de dominio
- **Cubre RFs:** los errores 400/404 de RF1–RF4.
- **Depende de:** nada.
- **Descripción:** crear `ValidacionTopicoException` (→ 400) y `TopicoNoEncontradoException` (→ 404) con mensajes legibles en español, según el plan (secciones 6 y 8).
- **Hecho cuando:**
  - [ ] ambas excepciones existen en el package service con mensajes en español; el proyecto compila.

## Tarea 6 — Mapper
- **Cubre RFs:** conversión entrada→entidad de RF1 y entidad→salida de RF1–RF5.
- **Depende de:** Tareas 2 y 3.
- **Descripción:** crear `TopicoMapper` como componente (vive en el package service) con `toDomain()` (entidad nueva sin id, campos sin espacios sobrantes; solo creación) y `toResponse()` (los 6 campos). El estado y la fecha se completan en el service, no aquí (plan, sección 5).
- **Hecho cuando:**
  - [ ] el mapper es un componente con exactamente dos métodos (`toDomain`, `toResponse`); `toDomain` no asigna id ni estado ni fecha; el proyecto compila.

## Tarea 7 — Service: crear (RF1)
- **Cubre RFs:** RF1; reglas R1 y R2.
- **Depende de:** Tareas 4, 5 y 6.
- **Descripción:** implementar `crear` en `TopicosService` siguiendo el plan (sección 6): validar obligatorios (null/""/solo espacios → 400), verificar R2 contra activos, `toDomain()`, completar estado ACTIVO y fecha de ahora, `save()`, `toResponse()`.
- **Hecho cuando:**
  - [ ] `crear` rechaza campos obligatorios vacíos (400) y duplicados titulo+mensaje activos ignorando mayúsculas (400); todo topico creado nace ACTIVO con fecha de ahora; el proyecto compila.

## Tarea 8 — Service: listar y detalle (RF2, RF3)
- **Cubre RFs:** RF2 y RF3; regla R5.
- **Depende de:** Tarea 7.
- **Descripción:** implementar `listar` (todos los activos ordenados por fecha descendente, mapeados con `toResponse`, array completo sin paginar) y `obtenerPorId` (activo por id; si no existe o está inactivo → 404), según el plan (sección 6).
- **Hecho cuando:**
  - [ ] `listar` devuelve solo topicos ACTIVO en orden de más recientes primero, sin paginación; `obtenerPorId` responde 404 para id inexistente o topico inactivo; el proyecto compila.

## Tarea 9 — Service: actualizar (RF4)
- **Cubre RFs:** RF4; reglas R2 y R3.
- **Depende de:** Tarea 8.
- **Descripción:** implementar `actualizar` (PUT parcial) según el flujo del plan (sección 6, paso a paso): buscar activo por id → 404; aplicar solo los campos con valor (null/""/espacios conservan la BD); si nada cambia → 200 sin cambios; verificar R2 sobre los valores finales excluyendo el propio id → 400; `save()` sobre la entidad cargada (JPA actualiza la fila porque conserva su id); `toResponse()` final.
- **Hecho cuando:**
  - [ ] `actualizar` modifica solo los campos con valor y conserva el resto de la BD; id, estado y fecha nunca cambian; body totalmente vacío → 200 sin cambios; duplicados con valores finales → 400; id inexistente/inactivo → 404; el proyecto compila.

## Tarea 10 — Service: eliminar (RF5)
- **Cubre RFs:** RF5; regla R4.
- **Depende de:** Tarea 9.
- **Descripción:** implementar `eliminar`: buscar activo por id → 404 si no existe o ya está inactivo; cambiar estado a INACTIVO; `save()` (borrado lógico, la fila permanece).
- **Hecho cuando:**
  - [ ] `eliminar` deja la fila en la BD con estado INACTIVO; no borra la fila físicamente; id inexistente o ya inactivo → 404; el proyecto compila.

## Tarea 11 — Controller
- **Cubre RFs:** exposición HTTP de RF1–RF5.
- **Depende de:** Tareas 2 y 7–10.
- **Descripción:** crear `TopicosController` con los 5 endpoints y códigos de la spec (sección 5): POST → 201 + `Location` + body; GET lista → 200 array; GET /{id} → 200; PUT → 200; DELETE → 204. Solo recibe/devuelve los dos DTOs; sin lógica ni referencias a la entidad (plan, sección 7).
- **Hecho cuando:**
  - [ ] existen los 5 endpoints con los códigos HTTP exactos de la spec; POST incluye la cabecera `Location` con el id creado; el controller no importa ni referencia la entidad `Topico`; el proyecto compila.

## Tarea 12 — Manejador de errores
- **Cubre RFs:** formato de error de RF1–RF5 (400/404 con `{"mensaje": "..."}`).
- **Depende de:** Tarea 5 (y coherentemente Tarea 11).
- **Descripción:** crear `ManejadorExcepciones` (@RestControllerAdvice) que traduce las dos excepciones de dominio a los errores JSON del contrato de la spec (sección 5), sin stack traces (plan, sección 8).
- **Hecho cuando:**
  - [ ] las excepciones del módulo responden con cuerpo `{"mensaje": "..."}` y estado 400 o 404 según corresponda; no se exponen stack traces; el proyecto compila.

## Tarea 13 — Verificación de compilación
- **Cubre RFs:** requisito transversal (criterio final de la spec, sección 9).
- **Depende de:** Tareas 1–12.
- **Descripción:** compilar el proyecto completo y resolver cualquier error; es la verificación obligatoria del proyecto (AGENTS.md).
- **Hecho cuando:**
  - [ ] `mvnw.cmd compile` termina sin errores.

## Tarea 14 — Verificación funcional (criterios de aceptación)
- **Cubre RFs:** RF1–RF5 completos.
- **Depende de:** Tarea 13.
- **Descripción:** arrancar la app con las variables de BD (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), dejar que Flyway aplique V1, y probar manualmente con un cliente REST los criterios de aceptación de la spec (sección 9), incluyendo los casos límite (CL1–CL7): PUT vacío → 200 sin cambios; strings de solo espacios conservan la BD; duplicados con distinta capitalización → 400; operaciones sobre topico inactivo → 404; crear duplicado de uno inactivo → permitido; DELETE dos veces → segunda vez 404.
- **Hecho cuando:**
  - [ ] los 10 criterios de aceptación de la spec (sección 9) quedan verificados y marcados, más los casos límite CL1–CL7; Flyway aplicó la migración V1 sin errores; se entregó al humano el resumen con la checklist final.
