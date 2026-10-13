# Spec · Frontend conectado a la API

Fecha: 2026-10-13 · Rama: `entrega-2-spec1` · Continúa [Frontend sobre puertos](2026-10-12-frontend.md) y las specs
[F1](2026-10-12-f1-certificaciones-parciales.md), [F2](2026-10-12-f2-versiones-vigencia-futura.md) y
[F3](2026-10-12-f3-politicas-jurisdiccion.md).

## 1. Objetivo

Que la interfaz funcione contra el backend real. Se escribe el adaptador HTTP detrás de los puertos que ya
existen, se elimina la implementación en memoria y se agregan a la API las pocas lecturas que las pantallas
necesitan y hoy no tiene. Las pantallas casi no cambian: es lo que la decisión D22 prometía.

Todo lo que esta spec dice de la API se comprobó contra el backend corriendo (60 llamadas de un flujo completo,
guardadas como archivos); no sale de leer el código.

### Criterios de aceptación

1. Con el backend levantado, `npm run dev` permite ejecutar desde las pantallas cada capacidad de la consigna y
   F1, F2 y F3, y lo cargado queda en la base.
2. Existe `certiflow-frontend/src/adapters/http/` con una implementación de cada puerto. `composition.ts` es el
   único archivo que lo conoce. Ningún archivo de `src/ui` importa de `src/adapters`.
3. `src/adapters/inmemory/` y `DemoControls` no existen más; la interfaz no tiene control de fecha.
4. Las acciones de cada recurso salen de los `_links` de la API, salvo las dos del activo indicadas en §3.4.
5. Un rechazo del servidor, sea de negocio o no, se muestra con su mensaje. Nunca "Something went wrong" si el
   servidor explicó el motivo.
6. `npm run seed` carga datos de demostración a través de la API.
7. La API gana las lecturas de §5, cada una con su test de API, y `mvn clean verify` sigue en verde.
8. CI corre tres jobs: `build` (sin cambios), `frontend` (tests rápidos, sin backend) y `e2e` (backend real).

### Fuera de alcance

- Autenticación, subida de archivos, paginación en pantalla.
- Mover el reloj del servidor: no se pueden demostrar una acción vencida ni un certificado expirado sin esperar.
- Servir el frontend desde el jar o habilitar CORS. Se corre en dos procesos (§6).
- Cambiar respuestas existentes de la API. Solo se agregan campos, links y endpoints.

---

## 2. Qué decide la API real

Estas son las particularidades que el adaptador tiene que absorber. Cada una tiene su test con la respuesta
capturada (§7).

| Hecho observado | Consecuencia en el adaptador |
|---|---|
| Los `href` de HAL mezclan absolutos (`http://localhost:8080/…`) y relativos (`/api/v1/…`); `Location` es absoluto | Todo `href` se reduce a ruta y query antes de usarlo. Seguir uno absoluto saltearía el proxy y fallaría por origen cruzado. |
| Una colección vacía no trae `_embedded`; la última página no trae `next` | `_embedded?.x ?? []`; las listas siguen `next` hasta que no esté, así llegan completas. |
| Los tipos del dominio se serializan tal cual: `jurisdictionCode: {value}`, `basis.partials[].value`, `basis.inspection.id.value`, `schemaReference.assetType.name` | Se desenvuelven en un único lugar (`certificationMapping`). |
| Los tipos sellados no traen discriminador: el alcance es `{kind: "WHOLE_ASSET"}` o `{code: {value}}`; la base es `{inspection}` o `{partials}` | Se distinguen por la clave presente. |
| La elegibilidad sin `?subsystem` de una versión con subsistemas responde **409** (`CertificationConflictException`), igual que emitir sin `subsystemCode` | La pantalla ya consulta por subsistema cuando la versión los declara; no se cambia. |
| La inspección llega plana (`inspector`, `plannedDate`, `sections: null`) y sus respuestas no dicen a qué sección pertenecen | `findInspection` son tres pedidos (inspección, respuestas, revisiones) más la versión del esquema, que es inmutable y se guarda en caché. |
| La persona de la bitácora llega plana (`performedById`, `performedByName`) | Se arma `Person`. |
| La severidad llega como nombre (`MAJOR`) en criterios y hallazgos y como etiqueta (`Major`) en el resumen; el tipo de evidencia adjunta llega como etiqueta (`Photo`) | Se normalizan contra el vocabulario (§5.2). |
| Un número enviado como `2.50` vuelve como `2.5` | Se muestra lo que devuelve el servidor. Al enviar se respeta lo que se escribió. |
| Los 400 de Spring traen `title: "Bad Request"` y `detail: "Failed to read request"`; cualquier campo de más es un 400 | El adaptador manda exactamente los campos del contrato. Los rechazos que no son de negocio tienen su propio tipo de error (§3.5). |
| Los instantes son reales, en UTC | Se muestran en la hora local del navegador y `effectiveFrom` se envía como instante real (§3.6). |

---

## 3. Adaptador HTTP

```
certiflow-frontend/src/adapters/http
├── HalClient.ts              fetch: URL base, headers, persona, errores, paginación, reducción de href
├── wire.ts                   tipos de lo que viaja por la API
├── actions.ts                tabla rel → acción
├── catalogMapping.ts  schemaMapping.ts  inspectionMapping.ts  findingMapping.ts  certificationMapping.ts
│                             funciones puras: de la respuesta de la API a los tipos de src/domain
├── HttpCatalogPort.ts  HttpSchemaPort.ts  HttpInspectionPort.ts  HttpFindingPort.ts
│   HttpCertificationPort.ts  HttpAuditPort.ts
├── createHttpCertiflow.ts    arma los seis puertos a partir de una URL base
└── __fixtures__/             respuestas reales capturadas del backend
```

### 3.1 Cliente

- `Accept: application/hal+json, application/problem+json`.
- La persona de cada comando (`answeredBy`, `closedBy`, `verifier`…) viaja en `X-Person-Id` y `X-Person-Name`. Las
  lecturas no los mandan.
- URL base configurable: vacía en el navegador (mismo origen, vía proxy) y absoluta para el script de datos y los
  tests contra el backend.

### 3.2 Cómo se arman las URLs

El adaptador **construye** la URL de cada recurso a partir de su identificador (`/api/v1/findings/{id}`), porque
las rutas del navegador llevan identificadores y no `href`. **Sigue links** para tres cosas: las acciones
disponibles (§3.4), la paginación (`next`) y los recursos que la respuesta señala (`schema-version`, `Location`).
No recorre la API desde `GET /api/v1`.

### 3.3 Correspondencia de puertos y endpoints

| Puerto | Llamadas |
|---|---|
| `CatalogPort` | `POST /assets` · `GET /assets/{code}` · `GET /assets?type=` |
| `SchemaPort` | `POST /schemas` · `POST /schemas/{type}/versions` · `GET /schemas/{type}` + `/versions` · `GET /schemas` (§5.1) · `GET /schemas/{type}/effective-version?at=` · `GET /vocabulary` (§5.2) |
| `InspectionPort` | `POST /inspections` · `GET /inspections?asset=&status=` · `GET /inspections/{id}` + `/responses` + `/revisions` · `PUT …/responses/{code}/answer` · `POST …/evidence` · `POST …/observations` · `POST …/closure` · `POST …/rectifications` · `GET …/act` |
| `FindingPort` | `GET /findings?asset=` · `GET /findings/{id}` · `POST …/corrective-actions` · `POST …/corrective-actions/{n}/verifications` · `GET /assets/{code}/findings-summary` |
| `CertificationPort` | `GET /inspections/{id}/certification-eligibility?subsystem=` · `POST /certificates` · `GET /certificates?asset=&subsystem=` · `GET /certificates/{n}` · `POST …/suspensions` · `POST …/renewals` · `POST /assets/{code}/global-certificates` · `POST …/global-renewals` · `GET …/document` |
| `AuditPort` | `GET /{recurso}/{id}/audit-entries` |

Las operaciones que devuelven un identificador (`assignInspection`, `issueCertificate`…) lo toman del cuerpo del
`201`. Cerrar y rectificar devuelven los hallazgos levantados leyendo el link `findings` del cuerpo, que es un
objeto cuando hay uno y un arreglo cuando hay varios.

### 3.4 Acciones desde los links

| Recurso | `rel` presente → acción |
|---|---|
| Inspección | `closure` → `closeInspection` · `rectifications` → `rectifyInspection` · `act` → `generateInspectionAct` |
| Respuesta de un criterio | `answer` → `recordAnswer` · `evidence` → `attachEvidence` · `observations` → `addObservation` |
| Decisión de elegibilidad | `certificates` → `issueCertificate` |
| Hallazgo | `corrective-actions` → `planCorrectiveAction` |
| Acción correctiva | `verifications` → `verifyCorrectiveAction` |
| Certificado | `suspensions` → `suspendCertificate` · `renewals` → `renewCertificate` · `global-renewals` → `renewGlobalCertificate` |
| Esquema | `publishSchemaVersion` siempre: la API no lo condiciona |
| Activo | `assignInspection` si su tipo figura en la lista de esquemas (§5.1) · `issueGlobalCertificate` por el link nuevo `global-certificates` (§5.4) |

La tabla vive en `actions.ts`. El puerto no se renombra para parecerse a los `rel`.

### 3.5 Errores

- `application/problem+json` cuyo `title` es una excepción de dominio conocida → `DomainError` con ese nombre,
  `detail` como mensaje e `ineligibilities` si las trae.
- Cualquier otro rechazo (400 de Spring, 404 de un recurso, 5xx, sin conexión) → `RejectedRequestError` con
  estado, título y detalle. Es un tipo nuevo de `src/ports`.
- `ErrorBanner` muestra el detalle en los dos casos. Sin conexión con el backend, dice que no se pudo contactar al
  servidor.

### 3.6 Fechas

`formatInstant` deja de forzar UTC y muestra la hora local del navegador. `instantFromLocalInput` convierte la
hora local elegida en un instante real (`new Date(valor).toISOString()`). Las fechas sin hora (`plannedDate`,
`dueDate`, vigencias) siguen viajando como `AAAA-MM-DD`.

### 3.7 Cambios en puertos y pantallas

- Se elimina `DemoControls` y el control "Today (demo)" del encabezado.
- `Jurisdiction` pierde `description`: las políticas del dominio solo exponen su código (ver §10, punto 2).
- `SchemaPort.vocabulary()` y el resto de las firmas no cambian.
- Las pantallas no cambian más allá de lo anterior. Si al conectar aparece otra diferencia, se corrige en el
  adaptador, no en la pantalla.

---

## 4. Lo que se elimina

- `src/adapters/inmemory/` completo (reglas, reloj, bitácora, políticas, datos de ejemplo y sus tests).
- `src/ports/DemoControls.ts`.
- `src/application.test.tsx`, que probaba la aplicación compuesta con los datos en memoria.

Consecuencia: el frontend ya no se puede usar sin el backend levantado.

---

## 5. Agregados a la API

Solo lecturas y campos nuevos. Ninguna respuesta existente cambia de forma. Siguen el patrón de la fase 1:
interfaz de caso de uso en `certiflow-domain`, implementación en `certiflow-application`, `@Bean` en
`UseCaseConfiguration`, controlador y test de API.

### 5.1 Lista de esquemas

`GET /api/v1/schemas` → colección HAL `schemas` con la misma representación de `GET /schemas/{assetType}`.
Nuevo `ListSchemasUseCase` e `InspectionSchemaRepository.findAll()`. `GET /api/v1` gana el link `schema-list`;
el link `schemas` con plantilla queda como está.

### 5.2 Vocabulario

`GET /api/v1/vocabulary` →

```json
{
  "severities": [{ "name": "MINOR", "label": "Minor", "outcomeWhenUnmet": "OBSERVED" }],
  "evidenceKinds": [{ "name": "PHOTO", "label": "Photo" }],
  "jurisdictions": [{ "code": "DEMO-STRICT", "policyCode": "STRICT" }]
}
```

Las jurisdicciones salen del mismo registro que resuelve las políticas (`CertificationPolicyResolver`), no de
una segunda lista: agregar una jurisdicción en `UseCaseConfiguration` la hace aparecer en el alta de activos sin
tocar el frontend, que es el criterio de aceptación de F3. Severidades y tipos de evidencia salen de
`StandardSeverity` y `StandardEvidenceKind`.

### 5.3 Hallazgos

- `FindingResource` gana `subsystemCode` (nulo si el hallazgo no tiene subsistema). Hoy la interfaz mostraría
  "afecta a todos los subsistemas" para un hallazgo que sí tiene uno: es un dato incorrecto, no uno faltante.
- Cada línea de `FindingsSummaryResource` gana `findingId` y `subsystemCode`, para poder enlazar el hallazgo
  desde el resumen. Implica agregarlos a `FindingLine` en el dominio.

### 5.4 Link de certificado global

`AssetResponse` gana el link `global-certificates` cuando el activo declara subsistemas. El servidor valida igual
al recibir el `POST`.

### 5.5 Lo que no se toca

- El detalle de la bitácora de una respuesta muestra el `toString` de Java (`EARTH = NumericAnswer[value=2.50]`).
  Se ve en el historial y en el acta. No se corrige acá porque cambia una salida existente (ver §10, punto 3).
- La etiqueta en lugar del nombre en la evidencia adjunta y el 409 de la elegibilidad se absorben en el adaptador.

---

## 6. Cómo corre

Dos procesos. El servidor de desarrollo de Vite reenvía `/api` al backend, así el navegador habla con un solo
origen y no hace falta CORS.

```bash
mvn -pl certiflow-web spring-boot:run
```

```bash
cd certiflow-frontend && npm run dev
```

- `vite.config.ts`: `server.proxy` y `preview.proxy` de `/api` hacia `CERTIFLOW_API_URL`, por defecto
  `http://localhost:8080`.
- **Datos de demostración:** `npm run seed` ejecuta `scripts/seedDemoData.ts` con `tsx` (dependencia de
  desarrollo nueva) contra `CERTIFLOW_API_URL`. Usa el adaptador HTTP, así que pasa por las mismas reglas que la
  interfaz. Si los datos ya están cargados, avisa y no hace nada.
- Contenido, todo con fecha de hoy:
  - dos activos del mismo tipo con el mismo hallazgo `MAJOR` abierto, uno en `DEMO-STRICT` (rechazado) y otro en
    `DEMO-CONDITIONAL` (certificado condicional por 30 días): la demostración de F3;
  - un activo con dos subsistemas, un parcial emitido, el otro bloqueado por un hallazgo `CRITICAL`, y otro activo
    con ambos parciales y su certificado global: F1;
  - un esquema con ocho criterios y dos versiones, la segunda con vigencia futura, y una inspección abierta
    asignada antes que conserva la primera: F2;
  - hallazgos de las tres severidades, una acción correctiva aceptada, otra rechazada y un certificado suspendido.

---

## 7. Tests

| Nivel | Dónde corre | Qué |
|---|---|---|
| Mapeo | job `frontend`, sin backend | Cada función de `*Mapping.ts` contra las respuestas capturadas de `__fixtures__/`: alcance y base por clave presente, valores desenvueltos, acciones según los links, colección vacía, link `findings` único y múltiple. |
| Cliente | job `frontend` | `HalClient` con `fetch` sustituido: headers de persona solo al escribir, reducción de `href`, seguimiento de `next`, `problem+json` → `DomainError`, lo demás → `RejectedRequestError`, sin conexión. |
| Pantallas | job `frontend` | Las actuales con puertos sustitutos escritos en el test (`InspectionDetailPage.test.tsx`, `ruleKinds`, `ErrorBanner`). |
| Contrato | job `e2e`, backend real | `ports/certiflowContract.ts` contra el adaptador HTTP. |
| Flujo por pantallas | job `e2e` | Los cuatro tests de `applicationFlow.test.tsx`, con los identificadores que devuelve el servidor en lugar de `INS-1`. |

Cambios en la suite de contrato:

- **Aislamiento.** La base es compartida y no se limpia entre tests. Cada test usa un sufijo único en los códigos
  de activo **y en el tipo de activo** (hay un solo esquema por tipo, S2). Es el único cambio de diseño de la suite.
- **Sin reloj.** Se eliminan los siete casos que mueven la fecha: acción vencida, vencimiento del certificado,
  suspensión que no detiene el calendario, vigencia nueva al renovar, versión programada que entra en vigencia,
  resumen con vencidas y documento con estado a la fecha. Los cubren los tests del backend, que sí controlan el
  reloj. F2 se sigue probando sin mover la fecha: una versión futura no afecta a una inspección nueva y
  `effectiveVersion` responde la versión programada.
- **Mensajes.** Donde un caso comparaba un texto de error del adaptador en memoria, pasa a comparar solo el
  nombre de la excepción.

Para correrlos en la máquina hay que tener el backend levantado: `npm run test:e2e`.

---

## 8. Integración continua

Job nuevo `e2e` en `ci.yml`, con los mismos disparadores:

1. servicio PostgreSQL 17, como el job `build`;
2. `mvn -B -q -DskipTests install` y arranque del jar de `certiflow-web` contra ese servicio;
3. espera a que responda `GET /api/v1`;
4. `npm ci` y `npm run test:e2e` en `certiflow-frontend/`.

`build` no cambia. `frontend` sigue sin necesitar backend. `e2e` no se agrega a los checks obligatorios de la
rama hasta verlo estable.

---

## 9. Documentación

- `DESIGN.md`: se actualiza D22 (el costo de las reglas duplicadas desaparece con el adaptador en memoria) y se
  agrega **D23. Adaptador HTTP y niveles de test**: dónde, por qué se construyen las URLs y se siguen los links
  para las acciones, alternativas descartadas (cliente que navega desde la raíz; conservar el modo en memoria) y
  costo (el frontend no corre sin backend; el adaptador depende de los nombres de campo del dominio que la API
  serializa directo). Se quita la limitación "Frontend sin API".
- `README.md`: cómo levantar los dos procesos, cargar datos y correr los tests contra el backend.
- `certiflow-frontend/README.md`: "Cómo se conecta la API" pasa a "Cómo corre".
- `CHANGES.md` y `docs/specs/README.md`: una entrada.
- Plan en `docs/plans/2026-10-13-frontend-api.md` antes de implementar.

---

## 10. Decisiones confirmadas (2026-10-13)

1. **URLs.** El adaptador arma la URL de cada recurso con su identificador y sigue links para acciones,
   paginación y recursos relacionados (§3.2). Se descarta el cliente que descubre todo desde `GET /api/v1`.
2. **Jurisdicción sin descripción.** El alta de activos muestra "DEMO-STRICT · policy STRICT". Queda anotado para
   el equipo (§11) que una descripción legible exigiría agregarla a `CertificationPolicy`.
3. **`toString` en la bitácora.** No se corrige en esta fase; queda anotado para el equipo (§11).
4. **Link `global-certificates`** en `AssetResponse` cuando el activo declara subsistemas (§5.4).
5. **Base vacía antes de los datos de demostración.** Se recrea la base `certiflow` local, que tenía los datos de
   las llamadas de captura.
6. **Job `e2e` no obligatorio** hasta verlo estable (§8).

---

## 11. Notas para el equipo

No son parte de esta fase. Quedan registradas porque se ven desde la interfaz.

- **Detalle de la bitácora de una respuesta.** `Inspection.recordAnswer` y `rectify` arman el detalle con el
  `toString` de la respuesta: `EARTH = NumericAnswer[value=2.50]`, `VALVE = OptionAnswer[option=Good]`. Aparece
  en el historial de la inspección y en el acta. Corregirlo cambia una salida del dominio que pueden estar
  comparando tests existentes.
- **Descripción de las políticas.** `CertificationPolicy` expone `code()` y `evaluate(context)`. Para que el alta
  de activos explique qué implica cada jurisdicción ("todo hallazgo bloquea, 12 meses") haría falta un texto en
  la política; hoy solo se puede mostrar su código.
- **Escala decimal.** Una medición enviada como `2.50` se guarda con su escala (la bitácora la muestra), pero la
  API la devuelve como `2.5`.
- **Elegibilidad del activo entero en un esquema con subsistemas.** `GET …/certification-eligibility` sin
  `?subsystem` responde 409 en lugar de una decisión rechazada; la spec de F3 preveía 200 con la decisión.
