# Plan — Implementación del Módulo Topicos (v1)

**Proyecto:** ForoHub · **Fecha:** 2026-11-13 · **Origen:** `spec-topicos.md` (v1.2), en esta misma carpeta · **Estado:** Aprobado para implementación

**A quién va dirigido:** a la IA que implementará el módulo. Este plan contiene el **cómo** (estructura, modelos, JSON y detalles técnicos). La especificación (`spec-topicos.md`) conserva el **qué** y el **porqué** (requisitos funcionales, reglas de negocio, casos límite). Leer ambos documentos antes de escribir código. No se debe escribir código hasta que el humano lo ordene; este documento es la guía técnica de esa implementación.

## 1. Estructura del módulo

```
src/main/java/JMR/SDD/ForoHub/Topicos/
├── controller/
│   ├── TopicosController.java            // 5 endpoints REST, sin lógica
│   ├── TopicoDTORequest.java             // record: titulo, mensaje, autor (todo menos id)
│   ├── TopicoDTOResponse.java            // record: id, titulo, mensaje, fechaCreacion, estado, autor
│   └── ManejadorExcepciones.java         // @RestControllerAdvice → errores {"mensaje": "..."}
├── entity/
│   ├── Topico.java                       // entidad JPA, tabla topicos
│   ├── EstadoTopico.java                 // enum ACTIVO / INACTIVO
│   └── TopicoRepository.java             // Spring Data JPA + consultas derivadas
└── service/
    ├── TopicosService.java               // reglas R1–R5 + validaciones
    ├── TopicoMapper.java                 // @Component: toDomain() / toResponse()
    ├── ValidacionTopicoException.java    // → HTTP 400
    └── TopicoNoEncontradoException.java  // → HTTP 404
```

Únicamente **dos DTOs** en todo el módulo (`TopicoDTORequest` y `TopicoDTOResponse`); la entidad JPA nunca sale del service y no aparece en el controller.

## 2. Modelo de datos técnico (tabla `topicos`)

| Columna | Tipo MySQL | Detalle |
|---|---|---|
| id | BIGINT AUTO_INCREMENT, PK | id autogenerado por JPA (notación `@GeneratedValue`, estrategia identidad) |
| titulo | VARCHAR(150) NOT NULL | obligatorio |
| mensaje | TEXT NOT NULL | obligatorio |
| fecha_creacion | DATETIME NOT NULL | solo se establece al crear; no actualizable |
| estado | VARCHAR(20) NOT NULL | enum persistido como texto: `ACTIVO` / `INACTIVO` |
| autor | VARCHAR(100) NOT NULL | texto libre, obligatorio |

- Migración: `V1__crear_tabla_topicos.sql` en `src/main/resources/db/migration/`, solo la tabla `topicos` (sin tablas de respuestas ni de usuarios). Restricción del proyecto: nunca editar una migración ya aplicada.
- Entidad JPA con mapeo estándar; Lombok para accesores (regla de AGENTS.md); id autogenerado por JPA con la notación correspondiente.

## 3. DTOs y JSON

- **TopicoDTORequest** (sirve para crear y actualizar): `titulo`, `mensaje`, `autor`. Sin id: lo genera la base de datos.
- **TopicoDTOResponse** (toda salida del backend): `id`, `titulo`, `mensaje`, `fechaCreacion`, `estado`, `autor`.
- `fechaCreacion` se serializa en formato ISO-8601 (comportamiento por defecto del serializador JSON del stack web); `estado` se serializa como texto del enum.
- Listado: array JSON directo de `TopicoDTOResponse` (sin wrapper de página).
- Errores: objeto `{"mensaje": "..."}` con texto legible en español, sin stack traces.

## 4. Repository (consultas derivadas, sin SQL manual)

- Consulta de activos ordenada por fecha de creación descendente (listado completo sin paginar).
- Búsqueda de topico activo por id (detalle, actualizar, eliminar).
- Verificación de duplicados: existe otro topico ACTIVO con mismo titulo + mensaje sin distinguir mayúsculas (R2 al crear).
- Variante de la anterior excluyendo un id dado (R2 al actualizar, para no chocar consigo mismo).

## 5. Mapper (`TopicoMapper`, @Component en el package service)

- `toDomain(TopicoDTORequest)` → entidad `Topico` nueva, **sin id**, campos normalizados (sin espacios sobrantes). Solo para creación.
- `toResponse(Topico)` → `TopicoDTOResponse` con los 6 campos de la entidad.
- El service completa `estado = ACTIVO` y `fechaCreacion = ahora` (R1 es lógica de negocio, no del mapper).

## 6. Service (`TopicosService`) — reglas → técnica

- Transaccionalidad a nivel de clase; las consultas de lectura marcadas como solo lectura.
- **crear (RF1):** validar obligatorios (null/""/solo espacios → 400); verificar R2 contra activos; `toDomain()`; completar estado y fecha; `save()`; `toResponse()`.
- **listar (RF2):** consulta de activos ordenada por fecha desc; mapear toda la lista con `toResponse()`; devolver array completo.
- **obtenerPorId (RF3):** búsqueda de activo por id; si no hay → 404.
- **actualizar (RF4, PUT parcial):** 1) buscar topico activo por id → 404 si no existe o está inactivo; 2) aplicar sobre la entidad cargada solo los campos con valor (null/""/espacios = conservar); 3) si ningún campo cambia, devolver 200 sin cambios; 4) verificar R2 sobre los **valores finales** excluyendo el propio id → 400; 5) `save()`: como la entidad conserva su `id`, JPA **actualiza la fila existente** (no inserta una nueva); 6) `toResponse()` sobre la entidad actualizada.
- **eliminar (RF5):** buscar activo → 404 si no hay; cambiar estado a `INACTIVO`; `save()`.
- Excepciones de dominio (`ValidacionTopicoException` 400, `TopicoNoEncontradoException` 404) con mensajes en español; el advice las convierte en los errores JSON del contrato.

## 7. Controller (`TopicosController`)

- Controlador REST con base `/topicos`; los 5 endpoints de la spec con sus códigos: POST → 201 + cabecera `Location` (`/topicos/{id}`) + body; GET lista → 200 array; GET /{id} → 200; PUT → 200; DELETE → 204.
- Recibe y devuelve solo los dos DTOs; cero lógica de negocio y cero referencias a la entidad.

## 8. Manejador de errores (`ManejadorExcepciones`)

- `@RestControllerAdvice`: handler para `ValidacionTopicoException` → 400; handler para `TopicoNoEncontradoException` → 404; cuerpo `{"mensaje": "..."}`.

## 9. Orden de implementación

1. Migración SQL `V1__crear_tabla_topicos.sql`
2. Entity: `EstadoTopico`, `Topico`
3. `TopicoRepository`
4. `TopicoMapper`
5. Excepciones de dominio
6. `TopicosService`
7. DTOs (los dos records)
8. `TopicosController`
9. `ManejadorExcepciones`
10. Verificación: `mvnw.cmd compile` → arrancar con variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (Flyway aplica V1) → probar manualmente los criterios de aceptación de la spec (sección 9).

## 10. Notas y riesgos técnicos

- Listado sin paginar: coste de escalabilidad aceptado por decisión del humano; si el volumen crece, requiere revisión de la spec.
- Stack: Java 21, Spring Boot 4.0.9 con starter webmvc; validación manual (sin starter de validación, para no tocar el `pom.xml`).
- Sin tests automatizados en esta versión (fuera de alcance declarado en la spec).
- Convención del proyecto: código y comentarios en español; packages en minúscula; compilar con `mvnw.cmd` es obligatorio al terminar.
