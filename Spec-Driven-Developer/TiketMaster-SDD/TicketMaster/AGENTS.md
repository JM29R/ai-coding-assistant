# AGENTS.md — TicketMaster

> Biblia de reglas para agentes de IA. Última actualización: 2026-11-20.
> Alcance: solo reglas y formato de checklist. No incluye contexto de stack ni comandos.

## 1. Ámbito de trabajo
- Todo el trabajo se realiza dentro de `D:\ai-coding-assistant\Spec-Driven-Developer\TiketMaster-SDD\TicketMaster`.
- Queda prohibido crear, modificar, mover, renombrar o eliminar archivos fuera de esa carpeta.
- Leer archivos de fuera solo como referencia: no copiar su contenido al proyecto ni exponerlo en respuestas.
- No ejecutar comandos que escriban o generen archivos fuera de la carpeta del proyecto.

## 2. Archivos y zonas protegidas
- `pom.xml`: no modificarlo sin autorización explícita.
- `src/main/resources/application.properties`: no modificarlo sin autorización explícita.
- Variables de entorno: no leerlas (`System.getenv`, archivos `.env`, valores resueltos de `${...}`),
  no imprimirlas en respuestas, logs ni tests, y no copiarlas a ningún archivo.
  Los placeholders `${...}` en `application.properties` se dejan tal cual, sin resolverlos.
- No modificar `.git/`, `.idea/` ni `target/`.
- Migraciones Flyway (`src/main/resources/db/migration/`):
  - Sí se pueden crear migraciones nuevas con el formato `V{n}__descripcion.sql`.
  - No modificar, renombrar ni eliminar una migración existente sin autorización explícita del usuario,
    porque alterarlas rompe MySQL.

## 3. Autorización y conflictos
- "Autorización explícita" significa una frase en la conversación que nombre el archivo o la acción
  y el cambio concreto. Ejemplo: «Autorizo modificar pom.xml para agregar X».
- La autorización vale solo para ese archivo/acción y ese cambio: no se extiende a otros archivos
  ni a toda la sesión.
- Un task aprobado no sustituye la autorización: si el task requiere tocar un archivo protegido, pedirla.
- Si una regla impide completar lo pedido: parar, explicar qué regla choca, proponer una alternativa
  que no la viole y esperar confirmación antes de continuar.

## 4. Checklist obligatorio
- Todo task que se genere se escribe como archivo `.md` dentro de `AI-Interactions/` y nace con una
  sección `## Checklist` cuyos ítems usan `- [ ]`.
- Toda respuesta final de una tarea incluye un resumen con el mismo formato de checklist.
- El modelo marca `- [x]` solo después de haber ejecutado y comprobado esa acción.
  Si no la realizó o no pudo verificarla, deja `- [ ]` y explica el motivo.
  Nunca marcar ítems por adelantado ni dar acciones por hechas.
- Los ítems deben ser concretos y verificables (ej.: `- [x] Agregado X en ruta Y`), nunca vagos.

Plantilla para tasks y resúmenes:

### Resumen
- Qué se hizo:
- Archivos tocados:
- Decisiones que requieren validación del usuario:

### Checklist
- [ ] Entendí el alcance y las reglas aplicables
- [ ] Confirmé que no tocaba archivos protegidos sin autorización
- [ ] No leí ni expuse variables de entorno
- [ ] Todo el trabajo quedó dentro de la carpeta del proyecto
- [ ] Revisé el resultado antes de marcar estos ítems
- [ ] Dejé constancia de lo que no pude verificar (si aplica)

## 5. Idioma
- Documentación, tasks, comentarios y respuestas al usuario en español.
- Los identificadores de código siguen el estándar Java en inglés.
