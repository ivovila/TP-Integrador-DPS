# Spec · Entrega 3 · Revisión asistida con IA y harness de evaluación

Fecha: 2026-11-07 · Rama: `entrega-3` · Consigna: [CertiFlow · Entrega 3](../consigna/certiflow-entrega-3.md) ·
Plan: [Entrega 3](../plans/2026-11-07-entrega-3-asistente-ia.md)

## 1. Objetivo

Un asistente basado en un modelo de lenguaje revisa una inspección abierta antes de cerrarla y señala no
conformidades que el texto libre describe y las respuestas no reflejan. Sus sugerencias las acepta o descarta el
inspector, queda trazado qué sugirió la IA y qué decidió la persona, y un harness mide su calidad contra un
conjunto dorado. Esta spec describe comportamiento futuro; no afirma que las clases propuestas ya existan.

### Criterios de aceptación

1. El inspector de una inspección abierta puede pedir una revisión; el asistente lee respuestas, observaciones y
   descripciones de evidencia, y devuelve sugerencias de hallazgo con criterio, severidad estimada, fundamento y citas.
2. Una sugerencia nunca cambia la inspección: no responde, no cierra, no rectifica, no levanta hallazgos y no
   certifica. Aceptarla solo registra la decisión.
3. Cada sugerencia se acepta o se descarta una vez, por una persona con rol `INSPECTOR`; descartar exige motivo.
4. Queda trazado, y se puede consultar después, qué sugirió la IA (con qué versión de prompt y qué modelo), qué
   descartaron los guardrails y por qué, y qué decidió el inspector, quién y cuándo.
5. Pedir una revisión de una inspección cerrada se rechaza.
6. El dominio y la aplicación no dependen del proveedor ni de Spring AI; ArchUnit lo verifica.
7. CI corre todo contra un stub determinista; el harness contra el modelo real corre por separado.
8. El harness ejecuta un conjunto dorado de 15 casos y reporta hallazgos correctos, severidades acertadas,
   hallazgos inventados y no conformidades omitidas; un cambio de prompt lo vuelve a correr y compara.
9. Si el proveedor no responde o responde algo que no cumple el esquema, el pedido falla con 503 y no se guarda nada.

### Fuera de alcance

- Analizar imágenes: no se guardan archivos, solo la referencia y la descripción de cada evidencia.
- Registrar como hallazgo un problema que no corresponde a ningún criterio: el dominio no tiene dónde.
- Redactar el acta: la consigna pide al menos una funcionalidad, y las métricas del harness miden hallazgos.
- Revisar inspecciones cerradas, aplicar una sugerencia automáticamente o administrar prompts por la API.

---

## 2. Reglas y supuestos de negocio

### 2.1 Qué lee el asistente

Por cada criterio dentro del alcance capturado de la inspección: código, texto, la regla escrita en palabras, la
respuesta dada (o "sin responder"), el resultado que calculó la regla, las observaciones y las descripciones de
evidencia, con su tipo. Nada de eso sale del agregado `Inspection`: lo arma un traductor (§3.2).

**No lee la severidad configurada de cada criterio** (decisión cerrada, §2.4), ni datos personales: autores y
fechas no forman parte de la entrada.

### 2.2 Qué es una sugerencia

`Suggestion(criterionCode, estimatedSeverity, rationale, quotes)`. Las citas son fragmentos **literales** del texto
de la inspección que motivan la sugerencia. Se numeran por posición dentro de la revisión, desde 1.

### 2.3 Guardrails al armar la revisión

La revisión no guarda la respuesta del modelo tal cual: la pasa por tres controles deterministas. Lo que no los
supera queda registrado como **rechazado por guardrail, con su motivo**, nunca se muestra como sugerencia y no se
puede aceptar.

| Control | Motivo registrado |
|---|---|
| El criterio no existe o no está en el alcance de la inspección | `OUT_OF_SCOPE` |
| El criterio ya es no conforme: la regla ya levanta ese hallazgo | `ALREADY_NONCONFORMING` |
| Alguna cita no aparece literal en las observaciones o descripciones de evidencia de la inspección | `UNSUPPORTED_QUOTE` |

El tercero es la red principal contra hallazgos inventados: una sugerencia tiene que apoyarse en algo que el
inspector escribió. "Literal" se compara después de normalizar mayúsculas y espacios, para no rechazar una cita
correcta por un cambio de formato; una paráfrasis no pasa. La cita puede venir de **otro criterio**: un inspector
escribe donde le queda cómodo, y "una estantería tapa el cartel" anotado en la altura de montaje es un problema de
señalización. Exigir que la cita sea del mismo criterio rechazaría justo esas detecciones. El harness mide cuántas
sugerencias atrapa cada control.

### 2.4 Severidad

El modelo **estima** la gravedad de lo que lee; no recibe la configurada. Si la recibiera, la métrica "severidades
acertadas" sería trivial. La pantalla muestra la severidad del criterio, que es la que valdrá si termina habiendo
hallazgo, y avisa cuando el modelo estimó otra. La severidad estimada es información para el inspector y para el
harness; nunca se usa para decidir nada.

### 2.5 Decisión humana

- Aceptar: registra `Acceptance(stamp)`. No modifica la inspección. Para que haya hallazgo, el inspector corrige la
  respuesta del criterio por el flujo de siempre y la regla lo levanta al cerrar (S3, S4).
- Descartar: registra `Dismissal(stamp, reason)`; el motivo no puede estar vacío.
- Una sugerencia decidida no se vuelve a decidir. Una revisión no se borra.
- Cerrar la inspección no exige haber pedido ni decidido revisiones: el asistente es una ayuda, no un paso
  obligatorio. Las sugerencias que quedaron pendientes siguen pendientes y consultables.

### 2.6 Trazabilidad

La revisión guarda: inspección, versión de prompt, modelo, quién la pidió y cuándo, las sugerencias, las rechazadas
por guardrail con su motivo, y cada decisión con su `Stamp`. La bitácora (`AuditLog<ReviewId>`) registra
`REVIEW_REQUESTED` (con cuántas sugerencias y cuántas rechazadas), `SUGGESTION_ACCEPTED` y `SUGGESTION_DISMISSED`
(con el motivo).

**El acta no cambia.** Incluir las revisiones en el acta obligaría a que Inspección conozca a Asistencia, al revés
del orden de contextos que verifica D30. La traza se ve desde la inspección por el link `assistant-reviews`, que
arma la capa web, la única que conoce a todos los contextos.

---

## 3. Modelo, puertos y casos de uso

Se conserva la arquitectura de las entregas anteriores y las reglas del plan (§2.6). Contexto nuevo `assistance`,
después de `inspection` en el orden de contextos: conoce la inspección solo por `InspectionId`, como Certificación.

### 3.1 Clases

| Capa | Agregar | Modificar |
|---|---|---|
| Dominio `assistance.model` | `InspectionAssistant` (puerto), `InspectionUnderReview`, `CriterionUnderReview`, `AssistantReply`, `ProposedFinding`, `AssistantReview`, `ReviewId`, `Suggestion`, `RejectedProposal`, `GuardrailRejection`, `SuggestionDecision` (`PendingDecision`, `Acceptance`, `Dismissal`), `PromptVersion`, `ModelId`, `AssistantReviewService`, `AssistantReviewFactory`, `AssistantReviewSnapshot`, `AssistanceAudit`, excepciones | — |
| Dominio `assistance.repository` | `AssistantReviewRepository` | — |
| Dominio `assistance.usecase` | `RequestAssistantReviewUseCase`/`Command`, `AcceptSuggestionUseCase`/`Command`, `DismissSuggestionUseCase`/`Command`, `FindAssistantReviewUseCase`, `ListAssistantReviewsUseCase`, `AssistantReviewHistoryUseCase` | — |
| Dominio `shared.model` | `Transactions` (puerto), `UnavailableException` (cuarta categoría) | — |
| Aplicación `application.assistance` | los `…UseCaseImpl`, `InspectionReviewTranslatorImpl`, `RuleInWords` (un `RuleDescriber`) | — |
| Infraestructura | `StubInspectionAssistant`, `SpringAiInspectionAssistant`, `PromptCatalog`, `SpringTransactions`, persistencia JPA de la revisión, `V7__assistant_reviews.sql` | `pom.xml` de infraestructura (Spring AI) |
| Web | `AssistanceConfiguration`, `AssistantReviewController`, recursos y requests | `ApiExceptionHandler` (503), `InspectionController` (link), `SharedConfiguration` (catálogo de auditoría), `ArchitectureRulesTest` |

### 3.2 El puerto

```java
public interface InspectionAssistant {
    AssistantReply review(InspectionUnderReview inspection);
}
```

- `InspectionUnderReview(InspectionId, AssetType, int schemaVersion, boolean closed,
  List<CriterionUnderReview> criteria)`: sabe si está abierta (`ensureOpen`) y responde a los guardrails.
- `CriterionUnderReview(CriterionCode, String text, String rule, String answer, Outcome outcome,
  List<String> observations, List<EvidenceUnderReview> evidence)`: la regla y la respuesta ya escritas en
  palabras; la respuesta la escribe `AnswerText`, que ya existe, y la regla un `RuleDescriber` nuevo
  (`RuleInWords`). `EvidenceUnderReview(kind, description)`.
- `AssistantReply(PromptVersion, ModelId, List<ProposedFinding>)`; `ProposedFinding(CriterionCode, Severity
  estimated, String rationale, List<Quote> quotes)`. Es lo que dijo el modelo, antes de los guardrails.
- Si el proveedor falla o su respuesta no cumple el esquema, el adaptador lanza `AssistantUnavailableException`.

`InspectionReviewTranslator` es otro puerto de `assistance.model`, con su implementación en la aplicación: el
único lugar que conoce a la vez la inspección y lo que el asistente lee, como `InspectionResultTranslator` (D16).
Tiene dos operaciones: `translate(InspectionId)` y `assetCodeOf(InspectionId)`, que es lo que se bloquea antes de
guardar (D27).

### 3.3 Casos de uso

| Caso de uso | Comando | Devuelve |
|---|---|---|
| `RequestAssistantReviewUseCase` | `RequestAssistantReviewCommand(InspectionId, Person requestedBy)` | `ReviewId` |
| `AcceptSuggestionUseCase` | `AcceptSuggestionCommand(ReviewId, int position, Person acceptedBy)` | — |
| `DismissSuggestionUseCase` | `DismissSuggestionCommand(ReviewId, int position, String reason, Person dismissedBy)` | — |
| `FindAssistantReviewUseCase` | `ReviewId` | la revisión |
| `ListAssistantReviewsUseCase` | `InspectionId` | sus revisiones |
| `AssistantReviewHistoryUseCase` | `ReviewId` | su bitácora (un historial por contexto, D7) |

### 3.4 La llamada al modelo va fuera de la transacción (decisión cerrada)

Una llamada al modelo tarda segundos, y `TransactionalUseCases` (D20) envuelve el caso de uso entero. Pedir una
revisión se escribe en tres pasos visibles:

```java
InspectionUnderReview inspection = transactions.within(() -> openInspection(command.inspectionId()));
AssistantReply reply = assistant.review(inspection);
return transactions.within(() -> record(command, reply));
```

- `Transactions` es un puerto del shared kernel (`<T> T within(Supplier<T> work)`), implementado con
  `TransactionTemplate` en infraestructura. Es el mismo principio que `AssetLock` (D27): **lo que un caso de uso
  necesita para ser correcto se lee en el caso de uso**. Este caso de uso es el único que la composición no
  envuelve con `TransactionalUseCases`; aceptar, descartar y las consultas sí, como todos los demás.
- El tercer paso bloquea el activo, vuelve a traducir la inspección y aplica los guardrails sobre esa lectura: si
  la cerraron mientras respondía el modelo, el pedido se rechaza con 409 y no se guarda la revisión.

**Descartadas:** *dejar la llamada dentro de la transacción*: una conexión de la base ocupada durante segundos
por cada pedido. *Dos casos de uso que orquesta el controller*: el flujo de negocio pasaría a la capa web.
*Envolver colaboradores internos con el proxy de D20*: funciona, pero el límite de la transacción quedaría
escondido en la configuración, que es justo lo que D27 corrigió.

### 3.5 Excepciones

| Excepción | Categoría | Estado HTTP |
|---|---|---|
| `AssistantReviewNotFoundException`, `SuggestionNotFoundException` | `NotFoundException` | 404 |
| `ReviewOfClosedInspectionException`, `SuggestionAlreadyDecidedException` | `RuleViolationException` | 409 |
| `DismissalReasonRequiredException` | `InvalidValueException` | 400 |
| `AssistantUnavailableException` | `UnavailableException` (nueva) | 503 |

Que el proveedor falle no es una regla de negocio, así que no entra en las tres categorías de D28. Se agrega una
cuarta, `UnavailableException`, con su método en `ApiExceptionHandler`: la categoría sigue decidiendo el estado,
que es el mecanismo de D28.

---

## 4. Persistencia y auditoría

- Migración nueva `V7__assistant_reviews.sql`; no se edita ninguna aplicada.
- Tabla `assistant_review`: id, `inspection_id`, versión de prompt, modelo, quién la pidió y cuándo.
- Lo inmutable de la revisión (sugerencias y rechazadas por guardrail) se guarda como JSON, como lo inmutable de
  los esquemas (D19). Las decisiones, que cambian, van en una tabla `suggestion_decision` (revisión, posición,
  tipo, autor, momento, motivo).
- El agregado se reconstruye desde su snapshot sin repetir los guardrails ni volver a auditar (D18).
- `AssistanceAudit` (`REVIEW_REQUESTED`, `SUGGESTION_ACCEPTED`, `SUGGESTION_DISMISSED`) se registra en el
  `AuditActionCatalog` de la composición.

## 5. API REST

| Operación | Rol | Respuesta |
|---|---|---|
| `POST /api/v1/inspections/{id}/assistant-reviews` (sin cuerpo) | `INSPECTOR` | 201 + `Location` de la revisión |
| `GET /api/v1/inspections/{id}/assistant-reviews` | lectores de inspecciones | revisiones de la inspección |
| `GET /api/v1/assistant-reviews/{reviewId}` | lectores de inspecciones | la revisión con sus sugerencias |
| `GET /api/v1/assistant-reviews/{reviewId}/suggestions/{position}` | lectores de inspecciones | una sugerencia |
| `POST …/suggestions/{position}/acceptance` (sin cuerpo) | `INSPECTOR` | 201 |
| `POST …/suggestions/{position}/dismissal` con `{"reason"}` | `INSPECTOR` | 201 |
| `GET /api/v1/assistant-reviews/{reviewId}/audit-entries` | lectores de inspecciones | bitácora |

- La revisión vive en su propio recurso, como un hallazgo (`/findings/{id}`) aunque nazca de una inspección.
- HAL: una inspección abierta ofrece `assistant-reviews`; una sugerencia pendiente ofrece `acceptance` y
  `dismissal`, y una decidida ninguno; una sugerencia ofrece `criterion` hacia la respuesta del criterio, que es
  donde el inspector la corrige.
- La sugerencia informa `criterionSeverity` y `estimatedSeverity` por separado, nunca solo una.
- Se actualiza `docs/openapi.yaml`.

## 6. Prompts versionados

- `certiflow-infrastructure/src/main/resources/prompts/inspection-review/v1.st`, `v2.st`… Una versión publicada no
  se edita: cambiar el prompt es agregar una versión, como una versión de esquema (F2) o una migración.
- `PromptCatalog` guarda la suma de verificación de cada versión publicada; un test falla si el texto de una versión
  existente cambia. Es la misma lección que la migración V1 editada.
- La versión activa sale de `certiflow.assistant.prompt-version`; el modelo, de las properties de Spring AI.

## 7. Harness

### 7.1 Conjunto dorado

`certiflow-harness/src/main/resources/golden/case-NN.json`, 15 casos sobre los dos esquemas de la demo
(`certiflow-frontend/scripts/seedDemoData.ts`): el matafuego (`PRESSURE`, `WEIGHT`, `SEAL`, `HOSE`, `CORROSION`,
`SIGNAGE`, `HEIGHT`, `TAG`) y el recipiente a presión (`RELIEF-VALVE`, `LEAKS`, `INSULATION`, `GROUNDING`, `PANEL`).

| Tipo | Casos | Qué mide |
|---|---|---|
| No conformidad escondida en una observación o descripción de evidencia | 10 | hallazgos correctos y severidades |
| Inspección limpia | 3 | hallazgos inventados |
| Trampa: negación ("no se observan fugas") o redacción ambigua | 2 | hallazgos inventados y límites |

Formato de un caso:

```json
{
  "id": "case-01",
  "kind": "hidden",
  "description": "Presión en rango pero la observación describe la manguera agrietada",
  "assetType": "Fire extinguisher",
  "criteria": [
    {"code": "PRESSURE", "answer": "12.5", "observations": [],
     "evidence": [{"kind": "PHOTO", "description": "Manómetro con la aguja en la zona verde"}]},
    {"code": "HOSE", "answer": "Good", "observations": ["La manguera tiene una grieta cerca del acople"], "evidence": []}
  ],
  "expected": [{"criterion": "HOSE", "severity": "MINOR"}]
}
```

El harness completa texto, regla y resultado de cada criterio a partir del esquema, así un caso solo escribe lo
que el inspector cargó. Las respuestas usan el texto que la regla sabe leer (`AnswerText`): un número con punto
decimal, `true` o `false`, o el nombre de la opción. Cada caso responde **todos** los criterios de su esquema, con
la evidencia obligatoria, para que ninguno quede pendiente por omisión. La severidad esperada es la configurada en
el criterio, que es la que tendría el hallazgo.

### 7.2 Métricas

Por caso y en total, sobre las sugerencias que superan los guardrails:

- **Hallazgos correctos**: sugerencias cuyo criterio está en `expected`.
- **Severidades acertadas**: de las correctas, las que estimaron la severidad esperada.
- **Hallazgos inventados**: sugerencias cuyo criterio no está en `expected`.
- **Omitidas**: criterios de `expected` sin sugerencia. La consigna no la pide, pero sin ella un asistente que no
  sugiere nada no inventa nunca y parece perfecto.

Además, cuántas propuestas atrapó cada guardrail. Cada caso corre **5 veces**; se reporta la media y el rango.

### 7.3 Ejecución

- Módulo `certiflow-harness`, con dependencia de dominio e infraestructura; no forma parte de la aplicación.
- Resultado en `harness/results/<versión de prompt>/<modelo>.json` y un resumen en Markdown, versionados.
- **CI:** un test del módulo corre el harness contra el stub con salidas conocidas y verifica las métricas exactas.
- **Modelo real:** `.github/workflows/harness.yml`, disparado por cambios en los prompts y a mano, con la API key como
  secreto; compara con la versión anterior y falla si alguna métrica empeora fuera del rango observado.

---

## 8. Pruebas

| Riesgo | Nivel y resultado esperado |
|---|---|
| Una sugerencia cambia la inspección | Dominio y escenario: aceptar no modifica respuestas, resultado ni hallazgos |
| El asistente revisa una inspección cerrada | Aplicación y API: 409, nada guardado |
| La inspección se cierra mientras responde el modelo | Aplicación: el tercer paso rechaza y no guarda |
| Sugerencia fuera del alcance, sobre un criterio ya no conforme o con una cita inventada | Dominio, parametrizado: queda rechazada por guardrail con su motivo y no se puede aceptar |
| Decidir dos veces, o descartar sin motivo | Dominio y API: 409 y 400 |
| El proveedor falla o responde fuera del esquema | Aplicación y API: 503, nada guardado ni auditado |
| La revisión se pierde al reiniciar | Repositorio (H2 y PostgreSQL): sugerencias, rechazadas y decisiones intactas |
| Se edita un prompt ya publicado | Infraestructura: falla la suma de verificación |
| Dominio o aplicación importan Spring AI | ArchUnit |
| Un rol que no es inspector pide o decide | API: 403 |
| Las métricas se calculan mal | Harness contra el stub, con valores exactos |
| La pantalla deduce acciones del estado | Frontend: botones solo según los rels |

## 9. Alternativas y decisión recomendada

| Alternativa | Ventaja | Costo / decisión |
|---|---|---|
| **Revisión antes del cierre que complementa a las reglas (elegida)** | lee lo que las reglas no ven; no duplica lógica | el asistente no puede registrar problemas sin criterio |
| La IA sugiere hallazgos desde cero, como en la consigna literal | más fiel al texto | duplica las reglas y puede contradecirlas |
| Borrador del acta | menos riesgo | el acta ya se genera; las métricas no aplican |
| Aceptar corrige la respuesta por el inspector | demo más corta | la IA propondría valores de respuesta concretos |
| Aceptar crea un hallazgo manual | flexible | concepto nuevo que rompe S4 |
| Darle al modelo la severidad configurada | menos errores visibles | la métrica de severidad pierde sentido |

## 10. Documentación y definición de terminado

`DESIGN.md`: decisiones nuevas (contexto y puerto, guardrails, transacciones explícitas, cuarta categoría de
excepción, prompts versionados, harness y CI), patrones no aplicados, tabla riesgo → test y limitaciones.
`README.md`, `CHANGES.md`, `docs/openapi.yaml` y diagrama PlantUML. Terminado cuando se cumplen §1 y §8 en CI, el
harness real corrió al menos con dos versiones de prompt y sus resultados están en el repo.
