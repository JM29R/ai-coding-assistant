# Especificación — Módulo Topicos (v1.2)

**Proyecto:** ForoHub · **Fecha:** 2026-11-13 · **Estado:** Aprobada

## 1. Resumen
Módulo que gestiona el ciclo de vida completo de los **Topicos** del foro: creación, consulta, actualización y eliminación lógica. Un Topico es la unidad principal del foro; las Respuestas (módulo futuro) se asociarán a él.

## 2. Alcance
**Incluye:** CRUD completo de Topicos, borrado lógico, listado completo sin paginación ni filtros, migración Flyway de la tabla `topicos`.
**No incluye:** módulo Respuestas, usuarios/autenticación, tests automatizados, seguridad, reactivación de topicos inactivos.

## 3. Modelo de datos

| Campo | Tipo lógico | Descripción |
|---|---|---|
| id | numérico autogenerado | Identificador único (PK) |
| titulo | texto, obligatorio | Título del topico |
| mensaje | texto, obligatorio | Contenido del topico |
| fechaCreacion | fecha-hora, obligatorio | Se asigna automáticamente al crear; no editable |
| estado | enumeración, obligatorio | `ACTIVO` / `INACTIVO`; por defecto `ACTIVO` |
| autor | texto, obligatorio | Nombre del autor, texto libre (sin módulo de usuarios aún) |

**Reglas de la tabla `topicos` (MySQL, migración `V1__crear_tabla_topicos.sql`):**
- Columnas en snake_case: `titulo`, `mensaje`, `fecha_creacion`, `estado`, `autor`.
- `estado` se guarda como texto (nombre del enum).
- `fechaCreacion` no actualizable; se establece solo en la creación.
- En esta migración no se crean tablas de respuestas ni de usuarios.

## 4. Reglas de negocio
1. **R1 — Creación:** todo topico nace con estado `ACTIVO` y `fechaCreacion` = ahora.
2. **R2 — No duplicados:** al crear y al actualizar, no se admite otro topico `ACTIVO` con la misma combinación título + mensaje, sin distinguir mayúsculas. → Error 400.
3. **R3 — Actualización (v1.2):** `PUT /topicos/{id}` es una **actualización parcial**: solo se modifican `titulo`, `mensaje` y `autor` para los campos que llegan con valor; los campos que llegan `null` o vacíos conservan el valor existente en la base de datos. Si todos los campos llegan `null` o vacíos, la respuesta es 200 con el topico sin cambios. `id`, `estado` y `fechaCreacion` nunca se editan por esta vía. **Definición de vacío:** se considera "vacío" todo string `null`, `""` o compuesto únicamente por espacios en blanco; en todos esos casos se conserva el valor existente en la base de datos.
4. **R4 — Borrado lógico:** DELETE no elimina la fila; cambia estado a `INACTIVO`.
5. **R5 — Visibilidad:** el listado y el detalle solo muestran topicos `ACTIVO`. Un topico inactivo se comporta como inexistente para las consultas (404 en detalle).

## 5. Contrato de API (base: `/topicos`)

| Método | Ruta | Éxito | Error | Descripción |
|---|---|---|---|---|
| POST | `/topicos` | 201 + body + `Location` | 400 | Crear topico |
| GET | `/topicos` | 200 (lista) | — | Listar activos |
| GET | `/topicos/{id}` | 200 | 404 | Detalle |
| PUT | `/topicos/{id}` | 200 | 400 / 404 | Actualizar |
| DELETE | `/topicos/{id}` | 204 | 404 | Borrado lógico |

**Listado (v1.2):** sin paginación ni filtros; devuelve un array JSON directo con todos los topicos `ACTIVO`, orden fijo por `fechaCreacion` descendente (más recientes primero). Advertencia aceptada: sin paginar, este endpoint escala mal con el crecimiento de datos.
```json
[
  { "id": 1, "titulo": "Duda sobre JPA", "mensaje": "...", "fechaCreacion": "2026-11-13T10:30:00", "estado": "ACTIVO", "autor": "JMR" }
]
```
**Request de creación:**
```json
{ "titulo": "Duda sobre JPA", "mensaje": "¿Cómo mapeo una relación uno a muchos?", "autor": "JMR" }
```
**Request de actualización (v1.2: parcial; los campos `null` o vacíos conservan el valor de la base de datos):**
```json
{ "titulo": "Nuevo título" }
```
**Formato de error (400/404):** cuerpo JSON `{"mensaje": "..."}` con texto legible en español, generado por un manejador central de excepciones; sin stack traces.

## 6. Validaciones de entrada (decisión v1: validación manual en el service, sin Bean Validation)
- Al crear: `titulo`, `mensaje` y `autor` son obligatorios (no nulos ni vacíos) → 400 si fallan.
- Al actualizar (v1.2): un campo `null`, `""` o solo espacios no es error; se conserva el valor existente en la base de datos. La regla R2 se verifica sobre los **valores finales** (nuevos + conservados), excluyendo el topico actualizado.
- Duplicados: regla R2 → 400.
- id inexistente o topico inactivo en detalle/actualizar/eliminar → 404.

## 7. Arquitectura del módulo (package `JMR.SDD.ForoHub.Topicos`)

**Flujo de datos (v1.2):** la entidad JPA nunca se expone en el controller; solo viaja entre service y repository.
```
FRONTEND → JSON → Controller(TopicoDTORequest) → Service (reglas de negocio)
  → TopicoMapper.toDomain() → Topico (entity) → Repository.save() → Topico (con id)
  → TopicoMapper.toResponse() → TopicoDTOResponse → JSON → FRONTEND
```
- **entity**: entidad JPA `Topico` (id autogenerado por JPA con `@GeneratedValue`), enum `EstadoTopico` y `TopicoRepository` (Spring Data JPA) con consultas de activos, ordenadas por fecha descendente, y verificación de duplicados.
- **service**: `TopicosService` con las reglas R1–R5 y validaciones manuales; `TopicoMapper` (@Component, vive aquí) con dos métodos: `toDomain(TopicoDTORequest) → Topico` (entidad sin id, lista para guardar) y `toResponse(Topico) → TopicoDTOResponse` (con id, tras salir del repository). El service completa estado y fecha al crear (R1 es lógica de negocio). Excepciones `ValidacionTopicoException` (400) y `TopicoNoEncontradoException` (404).
- **flujo de actualización (PUT, v1.2):** el service busca primero el topico activo por id; si no existe o está inactivo, lanza `TopicoNoEncontradoException` (404). Sobre esa misma entidad cargada aplica únicamente los campos con valor del `TopicoDTORequest` y llama a `save()`; como la entidad conserva su `id`, JPA **actualiza la fila existente** en lugar de insertar una nueva. La respuesta se construye con `TopicoMapper.toResponse()` sobre la entidad ya actualizada. `toDomain()` se usa **exclusivamente en la creación** (entidad nueva, sin id).
- **controller**: `TopicosController` con los 5 endpoints; recibe y devuelve únicamente los **dos DTOs del módulo** (records): `TopicoDTORequest` (todo menos id) y `TopicoDTOResponse` (id, título, mensaje, fecha de creación, estado, autor). El listado devuelve `List<TopicoDTOResponse>` como array directo (v1.2: no existe `PaginaTopicosDTO` ni paginación). `ManejadorExcepciones` (@RestControllerAdvice) traduce las excepciones a los errores JSON de la sección 5.
- El controller no contiene lógica de negocio ni referencias a la entidad `Topico`.

## 8. Migración Flyway
- `V1__crear_tabla_topicos.sql` en `src/main/resources/db/migration/`, creando la tabla `topicos` según la sección 3.
- Restricción de proyecto: nunca editar una migración ya aplicada.

## 9. Criterios de aceptación
- [ ] `POST /topicos` crea topico y devuelve 201 con `Location` y body completo.
- [ ] `POST` con título+mensaje duplicado activo → 400.
- [ ] `POST` sin título, mensaje o autor → 400.
- [ ] `GET /topicos` devuelve todos los topicos activos como array JSON, más recientes primero (v1.2: sin paginar).
- [ ] `GET /topicos/{id}` de activo → 200; de inexistente/inactivo → 404.
- [ ] `PUT /topicos/{id}` actualiza solo los campos enviados y conserva los demás valores de la base de datos; id, estado y fecha intactos; respeta R2 con los valores finales; body con todos los campos `null`, `""` o solo espacios → 200 sin cambios; id inexistente o topico inactivo → 404.
- [ ] `DELETE /topicos/{id}` devuelve 204 y el topico pasa a `INACTIVO`; id inexistente → 404.
- [ ] Errores 400/404 con cuerpo `{"mensaje": "..."}`.
- [ ] Migración Flyway aplicada sin errores al arrancar la app (requiere variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- [ ] Proyecto compila con `mvnw.cmd compile`.

## 10. Decisiones registradas en esta versión
1. `autor` obligatorio al crear (respuesta del humano, 2026-11-13).
2. Listado sin filtros ni búsqueda (respuesta del humano, 2026-11-13).
3. Validación manual en el service en lugar de Bean Validation, para no modificar `pom.xml` (regla de proyecto).
4. **v1.1 → eliminada en v1.2 — DTO de página propio:** dejaron de existir los DTOs de página y la paginación del listado; ahora el listado es un array JSON directo.
5. **v1.1 — Dos DTOs únicos y entidad encapsulada:** `TopicoDTORequest` y `TopicoDTOResponse` (records); el controller no expone la entidad JPA (respuesta del humano, 2026-11-13).
6. **v1.1 — Mapper como componente:** `TopicoMapper` (@Component en el package service) con `toDomain()` y `toResponse()`; responsable de traducir request → entidad y entidad → response (respuesta del humano, 2026-11-13).
7. **v1.1 → modificada en v1.2 — Actualización:** inicialmente reemplazo total; desde v1.2 es **actualización parcial**: los campos `null` o vacíos conservan el valor de la base de datos, y un PUT sin campos modificables responde 200 sin cambios.
8. **v1.1 → eliminada en v1.2 — Wrapper de página:** `PaginaTopicosDTO` ya no existe; el listado es un array JSON directo.
9. **v1.2 — Solo dos DTOs y listado sin paginación:** `TopicoDTORequest` y `TopicoDTOResponse` son los únicos DTOs del módulo; `GET /topicos` devuelve la lista completa sin paginar (respuesta del humano, 2026-11-13).
10. **v1.2 — Semántica del PUT parcial:** campos `null` o `""` conservan el valor de la base de datos; la regla R2 se verifica con los valores finales tras aplicar los cambios (respuesta del humano, 2026-11-13).
