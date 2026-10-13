# Spec · F2 · Versiones con vigencia futura

Fecha: 2026-10-12 · Rama: `entrega-2-spec1` · Consigna: [CertiFlow · Entrega 2](../consigna/certiflow-entrega-2.md)

## 1. Objetivo

Separar publicación de entrada en vigencia y seleccionar la versión que corresponde al instante de inicio
de cada inspección. La versión capturada nunca cambia, aunque se publique o entre en vigencia otra.
Esta spec describe comportamiento futuro sobre la base de fase 1.

### Criterios de aceptación

1. Publicar permite indicar `effectiveFrom` futuro; omitirlo equivale a publicación inmediata.
2. `publishedAt` lo asigna el servidor; el cliente no puede alterar el historial de publicación.
3. Antes de `effectiveFrom` se selecciona la anterior; exactamente en ese instante se selecciona la nueva.
4. Consultar un instante histórico devuelve la versión aplicable o informa que no había ninguna.
5. Se pueden programar varias versiones, con entradas en vigencia estrictamente crecientes.
6. Una inspección conserva número y contenido capturados, incluso tras persistir y recargar.
7. La selección depende de un `Clock` inyectado, sin jobs ni banderas de activación almacenadas.
8. API, auditoría y persistencia distinguen fecha de publicación y fecha de vigencia.

### Fuera de alcance

- Cancelar, reprogramar o editar versiones publicadas; publicación retroactiva; intervalos con fin manual.
- Activar por la fecha prevista de visita; migrar inspecciones abiertas; bitemporalidad o event sourcing.
- Fecha calendario sin zona: el contrato usa instantes ISO-8601 con offset.

---

## 2. Reglas y supuestos de negocio

### 2.1 Eje temporal

`SchemaVersion(number, sections, publishedAt, effectiveFrom, ...)` conserva sus datos inmutables.
`effectiveFrom >= publishedAt`. No se aceptan fechas retroactivas porque cambiarían la respuesta histórica.
La primera versión se publica con vigencia inmediata al crear el esquema; solo las nuevas pueden programarse.

Cada versión N rige en `[effectiveFrom(N), effectiveFrom(N+1))`; la última no tiene límite superior.
`versionAt(t)` selecciona la mayor fecha de vigencia menor o igual a `t`. Si `t` precede a la primera,
lanza `NoEffectiveSchemaVersionException`; no retorna la última como fallback.
Para una consulta futura se devuelve la versión programada según lo publicado hasta hoy: es una previsión,
no garantía de que nunca se publicarán más versiones. El response indica `resolvedAt`.

### 2.2 Publicación y conflictos

- Número consecutivo por publicación, conservando `SchemaVersionLog`.
- Toda nueva vigencia debe ser estrictamente posterior a la última vigencia publicada. Rechazar empates.
- Si v2 ya está programada para diciembre, publicar v3 para noviembre se rechaza. Evita renumeraciones y
  reemplazos implícitos. También se rechaza una publicación inmediata que adelante a una versión pendiente.
- Se aceptan varias versiones futuras en orden cronológico. Reprogramación es una evolución posterior explícita.
- `publishedAt` y el valor por defecto de `effectiveFrom` se obtienen del MISMO `clock.instant()`.
- Registrar usuario, número, ambas fechas y contenido publicado en el mecanismo de auditoría existente.
- Publicaciones concurrentes se serializan por esquema para no perder versiones ni duplicar números/fechas.

### 2.3 Inicio de inspección

Se mantiene S1 de DESIGN.md: la inspección se inicia al asignarse. La fecha prevista no selecciona versión.
`AssignInspectionUseCaseImpl` toma un solo `startedAt = clock.instant()`, resuelve `schema.versionAt(startedAt)`
y pasa ese mismo instante a `InspectionService.assign`. No consultar el reloj dos veces a ambos lados del límite.
La versión capturada incluye contenido, fechas y asociaciones F1; no se resuelve de nuevo al responder, cerrar,
rectificar, renovar certificados o reconstruir desde snapshots.

### 2.4 Ejemplo verificable

| Versión | Publicación | Vigencia |
|---|---|---|
| v1 | 2026-10-12T12:00:00Z | 2026-10-12T12:00:00Z |
| v2 | 2026-10-13T12:00:00Z | 2026-11-01T03:00:00Z |
| v3 | 2026-10-14T12:00:00Z | 2026-12-01T03:00:00Z |

Una asignación en `2026-11-01T02:59:59.999999Z` usa v1; en `03:00:00Z` usa v2.
Una visita prevista para diciembre pero asignada en octubre usa v1. Una inspección v1 cerrada en diciembre
sigue usando v1. `03:00Z` corresponde a medianoche con offset `-03:00`; el backend compara instantes UTC.

---

## 3. Modelo y casos de uso

| Capa | Agregar / modificar |
|---|---|
| Dominio Esquemas | `SchemaVersion.effectiveFrom`, `SchemaVersionLog.versionAt(Instant)`, validación de orden, `InspectionSchema.versionAt(Instant)` |
| Commands | `PublishSchemaVersionCommand` añade `Optional<Instant> effectiveFrom`; nuevo `FindEffectiveSchemaVersionUseCase` con tipo e instante |
| Aplicación | publicación toma reloj una vez y devuelve la versión PUBLICADA; asignación usa `versionAt`; consulta resuelve explícitamente |
| Infraestructura | entidad de versión, mapper, JSON/snapshots si incluyen fechas, locks de publicación |
| Web | request/response de versión, consulta temporal, enlace `current-version` resuelto con el reloj |

Eliminar `currentVersion()` sin tiempo de los caminos de negocio. Si se necesita la última publicación,
nombrar `latestPublishedVersion()` para diferenciarla de la vigente. El POST retorna la recién publicada
aunque todavía no rija; no usar `versionAt(now)` para armar esa respuesta.
Mantener dominio/aplicación libres de frameworks y no agregar un agregado temporal independiente.

---

## 4. Persistencia y migración

- Nueva migración Flyway: columna `schema_version.effective_from` NOT NULL en una base nueva, sin backfill.
- Mantener unicidad `(asset_type, number)`; añadir `(asset_type, effective_from)` en el esquema nuevo.
- No se migran datos históricos: el alcance autorizado es un sistema nuevo.
- Definir precisión común de microsegundos: normalizar instantes de creación/publicación/asignación antes de
  validar y persistir; una fecha que colisiona tras normalización se rechaza. El instante registrado es el usado
  para seleccionar. No admitir que memoria y DB elijan distinto en el límite.
- `SchemaVersionLog` restaurado valida números y calendario consistente sin volver a auditar publicaciones.
- Una inspección recargada conserva su FK/snapshot de versión; nunca apunta a «la vigente» por consulta dinámica.
- Bloqueo pesimista de fila del esquema en publicación dentro del proxy transaccional existente. Restricciones
  reales como defensa adicional; conflicto de publicación responde 409. Auditoría revierte con la transacción.

---

## 5. API REST

| Operación | Contrato |
|---|---|
| `POST /api/v1/schemas/{assetType}/versions` | `{sections: [...], effectiveFrom: "2026-11-01T00:00:00-03:00"}`; campo temporal opcional; 201 + Location de versión publicada |
| `GET /api/v1/schemas/{assetType}/effective-version?at=...` | instante obligatorio con offset, 200 con versión y `resolvedAt` |
| `GET /api/v1/schemas/{assetType}` | `current-version` calculado con Clock; diferenciar `latest-published-version` |
| `GET /api/v1/schemas/{assetType}/versions/{number}` | incluye `publishedAt` y `effectiveFrom`; representación inmutable y ETag existentes |

En ejemplos JSON reales se usan comillas en todas las propiedades. Este cuadro abrevia el contrato.
400: formato temporal inválido/sin offset o vigencia anterior a publicación. 404: esquema inexistente o
fecha anterior a toda versión, con tipo `NoEffectiveSchemaVersionException`. 409: empate/desorden respecto
a lo ya publicado o carrera de publicación. Un request rechazado no agrega versión ni auditoría.
El recurso de esquema/consulta vigente no usa una caché inmutable que sobreviva al próximo límite; usar
`no-cache` con revalidación temporal o `no-store`. Las versiones por número sí mantienen caché inmutable.

---

## 6. Pruebas y demostración

| Escenario | Nivel / expectativa |
|---|---|
| antes, exactamente en y después del límite | dominio parametrizado con precisión persistida: v1/v2/v2 |
| anterior a primera versión | dominio/API: sin versión vigente, 404 |
| varias futuras y consulta después de v3 | dominio: intervalos semiabiertos y v3 sin fin |
| offset equivalente | API: `00:00-03:00` y `03:00Z` resuelven igual |
| publicación inmediata sin campo | aplicación: ambas fechas iguales, publicación inmediata |
| fecha prevista posterior | aplicación con MutableClock: selecciona por asignación |
| v1 abierta al activar v2 | aplicación/repositorio/API: respuestas/cierre y rectificación siguen v1 |
| empate, retroactividad, orden inválido | dominio/API: error previsto, sin escritura ni auditoría |
| reloj cambia durante command | aplicación: se toma una sola vez para seleccionar y registrar |
| ida y vuelta y límite de microsegundos | repositorio H2/PostgreSQL: mismas fechas y misma selección |
| dos publicaciones concurrentes | repositorio real: log sin pérdidas, números únicos; una falla si fechas iguales |

Demo con Clock controlado, no esperas reales: publicar v2 futura, asignar antes y en el límite, cerrar ambas
y consultar historia. Ejecutar `mvn -q clean verify` y probar migraciones de base vacía en PostgreSQL del compose.

---

## 7. Alternativas y decisión recomendada

| Alternativa | Ventaja | Costo / decisión |
|---|---|---|
| **resolver por `effectiveFrom` al consultar (recomendada)** | determinista, histórica, sin proceso de activación | selección temporal y cuidado de caché |
| scheduler actualiza puntero `current` | lectura simple | fallas/retrasos del job, carreras y recuperación tras reinicio |
| flag `active` por versión | fácil al principio | no responde historia por sí solo y puede haber dos activas |
| intervalos explícitos `from/until` | permite huecos y cancelaciones | editar anterior al publicar y controlar solapamientos; no requerido |
| bitemporalidad/event sourcing | distingue qué se sabía en cada fecha | complejidad sin requisito de publicación retroactiva |

Recomendar historial inmutable y orden estricto de vigencia: cumple F2 con una extensión acotada del modelo actual.
No añadir State ni jobs para un estado que se obtiene comparando fechas.

## 8. Documentación y definición de terminado

Actualizar S1/S2 y glosario en DESIGN.md: «última publicada» difiere de «vigente»; registrar clases/refactorizaciones
reales y concesión de no reprogramar. README/CHANGES y ejemplos deben usar instantes con offset. Terminado al
pasar §1 y §6 en CI, con migración comprobada. Ver [coordinación F1–F3](README.md).
