# Spec — Evento (TicketMaster)

> Versión aprobada · 2026-11-22
> Ubicación: `AI-Interactions/spec-Evento/spec-Evento.md`
> Esta spec define reglas de negocio y criterios de aceptación. La solución técnica se definirá en `plan.md`.

## 1. Propósito y alcance

Un **Evento** es el contexto que permite registrar **Tickets** para una ocasión determinada.
Los Tickets son otra entidad y se especificarán por separado; aquí solo se define el Evento y las
reglas que el registro de tickets deberá respetar.

Incluido:
- Crear un Evento.
- Publicar un Evento (cambio de estado simple: Borrador → Publicado).
- Consultar Eventos: listado, búsqueda y filtros.
- Dar de baja lógica un Evento.

Fuera de alcance:
- Editar los datos de un Evento.
- Cancelar un Evento como operación (el estado Cancelado existe, su transición se definirá después).
- Eliminar físicamente un Evento.
- Definir los datos o el flujo de la entidad Ticket.
- Autenticación, roles o control de acceso.

## 2. Datos del Evento

- **Título**: obligatorio; texto que identifica al evento.
- **Descripción**: obligatoria.
- **Inicio**: obligatorio; fecha y hora.
- **Fin**: obligatorio; fecha y hora.
- **Ubicación**: obligatoria; texto libre con el lugar del evento.
- **Aforo**: obligatorio; cantidad máxima de tickets registrables. Debe ser al menos 1.
- **Registrador**: obligatorio; texto simple que indica quién registró el evento. Sin login ni validación de identidad.
- **Identificador**: se asigna al crear el evento y permite buscarlo.
- **Estado declarado**: Borrador | Publicado | Cancelado | Eliminado. Se asigna explícitamente.
- **Estado calculado**: En curso | Finalizado. Se obtiene al consultar o validar, comparando la fecha/hora
  actual contra Inicio y Fin.

Cálculo del estado efectivo:
- Publicado y ahora anterior a Inicio → Publicado.
- Publicado y ahora entre Inicio y Fin (inclusive) → En curso.
- Publicado y ahora posterior a Fin → Finalizado.
- Borrador, Cancelado y Eliminado conservan su estado declarado sin importar las fechas.

> Eliminado es la baja lógica: el registro se conserva, nunca se borra.

## 3. Reglas de negocio

### Creación
- Al crear, el Evento nace en estado Borrador.
- Todos los datos obligatorios deben estar completos.
- El fin debe ser posterior al inicio.
- El inicio no puede estar en el pasado al dar de alta.
- No se define quién puede crear eventos; no hay control de acceso en esta spec.

### Publicación
- La operación de Publicar solo se aplica a un Evento en estado Borrador.
- Publicar cambia el estado a Publicado; no modifica ningún otro dato.
- No se puede Publicar si el Inicio ya pasó: la operación se rechaza y el Evento sigue en Borrador.
- No hay restricción de actor para Publicar (sin login ni roles).

### Estados, baja lógica y tickets
- Solo los estados efectivos Publicado y En curso admiten tickets nuevos.
- Borrador, Finalizado, Cancelado y Eliminado no admiten tickets nuevos.
- Cancelado sigue siendo visible en las consultas normales.
- Eliminado deja de aparecer en consultas normales y no admite tickets nuevos. Es definitiva en esta spec.
- Dar de baja no elimina el registro: solo cambia su estado declarado a Eliminado.
- Los tickets ya registrados en un Evento dado de baja se conservan como histórico.
- Dar de baja no tiene restricción de actor.

### Consulta
- El listado normal incluye solo Eventos cuyo estado declarado no sea Eliminado.
- Si se filtra explícitamente por Eliminado, esos Eventos se incluyen; sin filtro, el listado normal los excluye.
- El estado mostrado es el efectivo: los Eventos Publicados pueden aparecer como Publicado, En curso o Finalizado según la fecha/hora.
- Se puede buscar un Evento por identificador o por título.
- Se pueden aplicar filtros por estado (declarado o calculado) y por rango de fechas.
- Existe una consulta explícita y separada para Eventos Eliminados, sin control de acceso.
- Si no hay coincidencias, se informa "sin resultados" o "no encontrado", sin otro efecto.

### Tickets (solo lo que el Evento impone)
- No se pueden registrar tickets que superen el aforo del Evento.
- No se pueden registrar tickets en Eventos cuyo estado efectivo no sea Publicado o En curso.

## 4. Casos límite y errores

- Falta un dato obligatorio → no se crea el Evento y se informa qué dato falta.
- Fin igual o anterior al inicio → no se crea el Evento.
- Inicio en el pasado → no se crea el Evento.
- Aforo menor a 1 → no se crea el Evento.
- Intento de Publicar un Evento que no está en Borrador → se rechaza y se informa.
- Intento de Publicar con Inicio ya pasado → se rechaza, el Evento sigue en Borrador y se informa.
- Evento inexistente → se informa "no encontrado".
- Filtro sin coincidencias → se informa "sin resultados".
- Sin cupo disponible → no se registra el ticket y se informa.
- Registrar ticket en Evento Eliminado, Cancelado, Borrador o Finalizado (calculado) → se rechaza y se informa.
- Dar de baja un Evento ya Eliminado → no cambia nada y se informa.

## 5. Criterios de aceptación

### Crear Evento
- [ ] Dado que completo todos los datos obligatorios, cuando creo el Evento, entonces se crea en Borrador, recibe un identificador y puedo consultarlo.
- [ ] Dado que omito la descripción, cuando intento crear el Evento, entonces no se crea y se me informa el dato faltante.
- [ ] Dado que el fin es anterior al inicio, cuando intento crear el Evento, entonces no se crea y se informa el problema de fechas.
- [ ] Dado que el aforo es 0, cuando intento crear el Evento, entonces no se crea y se informa que el aforo debe ser al menos 1.
- [ ] Dado que el registrador se indica como texto simple, cuando creo el Evento, entonces se conserva ese texto sin pedir login.

### Publicar Evento
- [ ] Dado que el Evento está en Borrador con inicio futuro, cuando lo publico, entonces pasa a Publicado y puedo consultarlo.
- [ ] Dado que el Evento está en Borrador pero su inicio ya pasó, cuando intento publicarlo, entonces se rechaza, sigue en Borrador y se informa.
- [ ] Dado que el Evento está Publicado o Eliminado, cuando intento publicarlo, entonces se rechaza porque la operación solo aplica a Borrador.
- [ ] Dado que publico el Evento, cuando consulto sus datos, entonces ningún dato salvo el estado fue modificado.

### Consultar Eventos y estados calculados
- [ ] Dado que existen Eventos en varios estados, cuando listo Eventos, entonces aparecen todos salvo los Eliminados.
- [ ] Dado que un Evento Publicado tiene fecha actual anterior a su Inicio, cuando lo consulto, entonces se muestra como Publicado.
- [ ] Dado que un Evento Publicado tiene la fecha actual entre Inicio y Fin, cuando lo consulto, entonces se muestra como En curso.
- [ ] Dado que un Evento Publicado tiene la fecha actual posterior a su Fin, cuando lo consulto, entonces se muestra como Finalizado.
- [ ] Dado que un Evento está Eliminado, cuando hago la consulta normal, entonces no aparece; cuando filtro explícitamente por Eliminado en el listado, entonces aparece; cuando hago la consulta explícita de eliminados, entonces aparece.
- [ ] Dado que aplico filtros por estado y rango de fechas, cuando consulto, entonces obtengo solo los Eventos que cumplen ambos filtros.
- [ ] Dado que busco un Evento inexistente, cuando consulto, entonces recibo "no encontrado" y no se modifica nada.

### Dar de baja lógica
- [ ] Dado que un Evento está Publicado, cuando lo doy de baja, entonces su estado pasa a Eliminado, sigue existiendo y desaparece del listado normal.
- [ ] Dado que un Evento tiene tickets registrados, cuando lo doy de baja, entonces los tickets se conservan como histórico.
- [ ] Dado que un Evento está Eliminado, cuando intento registrar un ticket, entonces se rechaza y se informa.
- [ ] Dado que un Evento Finalizado se calculó por fechas, cuando intento registrar un ticket, entonces se rechaza y se informa.
- [ ] Dado que un Evento ya está Eliminado, cuando lo doy de baja otra vez, entonces no cambia nada y se informa.

### Tickets (invariantes del Evento)
- [ ] Dado que el aforo ya está completo, cuando intento registrar un ticket, entonces se rechaza por falta de cupo y se informa.
- [ ] Dado que el Evento está en Borrador, Cancelado o Eliminado, cuando intento registrar un ticket, entonces se rechaza y se informa.

## 6. Checklist de cierre para quien implemente

- [ ] Leí esta spec completa antes de implementar.
- [ ] Usé viñetas normales en secciones descriptivas y checkboxes solo en 5 y 6.
- [ ] Respeté que En curso y Finalizado se calculan, no se asignan.
- [ ] Marqué cada criterio de aceptación solo después de verificarlo.
- [ ] No implementé la entidad Ticket ni su flujo.
- [ ] No agregué autenticación, roles ni control de acceso.
- [ ] Señalé cualquier decisión tomada que no estuviera en esta spec.
