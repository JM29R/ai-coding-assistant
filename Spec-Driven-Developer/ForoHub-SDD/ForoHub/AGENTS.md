# AGENTS.md — ForoHub

> Documento de contexto para agentes de IA. Última actualización: 2026-11-13.

## Proyecto
- Foro simple constituido por **Topicos** y **Respuestas** a esos topicos.
- **Topico** es la entidad principal. Una **Respuesta** es un aporte o contestación asociada a un Topico.
- Stack: Java 21, Spring Boot 4.0.9 (spring-webmvc, data-jpa, flyway), Lombok, MySQL.
- Estado actual: proyecto recién inicializado. Solo existe `ForoHubApplication.java`; los módulos aún no están implementados.

## Especificaciones (SDD)
- Las especificaciones viven en `AI-Interactions/` (ajustar si se usa otra carpeta).
- Flujo de trabajo obligatorio: **especificar → planear → implementar → verificar**.
- Antes de implementar una feature, leer la especificación correspondiente. Si no existe, proponer un borrador y esperar confirmación antes de codificar.
- Si una decisión de diseño no está en la especificación, preguntar; no inventar reglas de negocio.

## Comandos (Windows) REALIZAR SOLO AL IMPLEMENTAR CODIGO NO .md
- Ejecutar: `mvnw.cmd spring-boot:run`
- Compilar: `mvnw.cmd compile`
- Tests: `mvnw.cmd test`
- Empaquetar: `mvnw.cmd package -DskipTests`
- No hay linter configurado aún (no existe comando de lint/formato).


## Modelo de datos (intención)
- `Topico`: id , título, mensaje, fecha de creación, estado, autor.
- `Respuesta`: id , mensaje, autor, fecha, referencia al Topico.
- Relación: una Respuesta pertenece a un Topico (muchas respuestas → un topico).
- Todos los id son autos generados por JPA con la notacion correspondiente.
- 
## Estilo y convenciones
- Escribir código y comentarios en **español**; responder al usuario en español.
- Lombok para getters/setters/constructores; no escribir código repetitivo a mano.
- `Topico` es la clase titular; `Respuesta` es la clase de respuesta a `Topico`.
- Nombres de packages: `Topicos`, `respuestas`.
- Validación de entrada con Bean Validation (`jakarta.validation`).

## Arquitectura
- Monolito modular: cada módulo se separa en carpetas `controller`, `entity` y `service`.
- Ubicación de los módulos (packages Java, aún no creados):
  - Módulo Topicos: `src/main/java/JMR/SDD/ForoHub/Topicos/`
  - Módulo Respuestas: `src/main/java/JMR/SDD/ForoHub/respuestas/`
- `service`: lógica de negocio y mappers (entidad ↔ DTO).
- `entity`: entidad JPA y su Repository (Spring Data JPA).
- `controller`: controlador REST y DTOs de request/response.

## Reglas
- No editar `pom.xml` sin consultar.
- No editar `src/main/resources/application.properties` sin consultar.
- **Flyway**: no editar una migración ya aplicada; siempre crear una nueva migración `V{n}__descripcion.sql` en `src/main/resources/db/migration/`.
- No trabajar fuera de la carpeta `D:\ai-coding-assistant\Spec-Driven-Developer\ForoHub-SDD\ForoHub`.
- No tocar archivos dentro de `target/` ni `.idea/`.

## Testing
- Usar los starters de test ya incluidos en el pom (`spring-boot-starter-*-test`, JUnit 5).
- Tests en `src/test/java/JMR/SDD/ForoHub/`, replicando la estructura de packages del módulo testeado.
- Compilar y pasar los tests es parte de la verificación obligatoria antes de terminar una tarea.

## Al terminar cualquier tarea
- Verificación propia obligatoria antes de terminar: compilar (`mvnw.cmd compile`) y, si aplica, correr tests.
- Informar qué se realizó, con un pequeño resumen y una checklist para revisar lo implementado.
- Señalar cualquier decisión tomada sin especificación para que el humano la valide.
