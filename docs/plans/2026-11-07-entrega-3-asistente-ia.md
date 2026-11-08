# ENTREGA-3 · Asistente con IA y harness de evaluación · Plan

> **Estado al 2026-11-08: fase 4 implementada, integración real NO validada.** Los checks marcados son
> implementación y tests con stub/servidor local, no una prueba exitosa con NVIDIA. No hubo una generación
> real: timeouts y HTTP 504. Para retomar ahora, leer el
> [traspaso con pasos pendientes](../handoffs/2026-11-08-fase-4-asistencia-pendiente.md).

**Consigna:** [CertiFlow · Entrega 3](../consigna/certiflow-entrega-3.md), con las indicaciones de entrega de la cátedra al final.

**Arquitectura:** los cuatro módulos DDD existentes, más un módulo `certiflow-harness` fuera de la aplicación.
Dominio y aplicación siguen sin frameworks ni proveedores externos.

**Decisiones ya tomadas por el equipo (2026-11-07):**

- La propuesta **aplica**, reencuadrada (ver §1). No se entrega justificación de no aplicación.
- Aceptar una sugerencia **solo se traza**: no escribe nada en la inspección. El inspector corrige la respuesta y
  la regla levanta el hallazgo al cerrar, como siempre.
- Se hace el **frontend**: panel en la inspección abierta para la demo del video.
- Sin fecha límite impuesta: el plan se ordena por fases, no por días.
- El adaptador real usa **Spring AI 2.0**, solo en infraestructura (§2.4). El proveedor concreto pasa a ser
  configuración.

**Formato de entrega (indicaciones de la cátedra, más allá del PDF):** dos enlaces. El repositorio con el hash del
commit de la entrega, y un **video de YouTube no listado de 8 a 12 minutos** donde aparecen los cinco integrantes,
cada uno hablando de al menos una parte distinta del sistema. Sin el video no se evalúa el TPI. Contenido mínimo:
decisiones de dominio, implementación de los endpoints, refactors entre entregas y propuesta de valor; **para cada
uno, las alternativas exploradas y por qué se eligió la que se eligió.**

**Proveedor elegido (2026-11-08):** NVIDIA NIM, a pedido del usuario, con `google/gemma-4-31b-it`. La key fue aportada para la prueba, sin persistirla. El harness completo sigue pendiente (§5.4).

---

## 1. Por qué aplica, y en qué forma

La consigna pide sugerir hallazgos y severidades a partir de observaciones y evidencias. En Certiflow eso, leído
al pie de la letra, duplicaría lógica de negocio: los hallazgos y su severidad no son un juicio, los calculan las
reglas (`Criterion.outcomeOf` → `Severity.outcomeWhenUnmet` → un hallazgo por no conformidad al cerrar, S3 y S4).

Lo que las reglas no ven es el **texto libre**. Una regla mira la respuesta estructurada (número, sí/no, opción);
las observaciones y las descripciones de evidencia no las lee nadie. Caso típico: "presión en rango: sí" con la
observación "la manguera está agrietada". La regla aprueba, no hay hallazgo, y el problema queda escrito sin
consecuencia.

**Funcionalidad elegida: revisión asistida antes del cierre.** El asistente lee respuestas, observaciones y
descripciones de evidencia de una inspección abierta y señala no conformidades que el texto describe y las
respuestas no reflejan. Complementa a las reglas, no las reemplaza.

**Descartada: borrador del acta.** El acta ya se genera sola y es estructurada (`InspectionAct`), y las métricas
que pide la consigna (hallazgos correctos, severidades acertadas, hallazgos inventados) miden sugerencias de
hallazgos, no texto narrativo.

**Límites que se asumen y se presentan:**

- No se guardan imágenes: la evidencia es `(kind, reference, description)`. El asistente lee descripciones.
- Un problema que no encaja en ningún criterio del esquema no puede ser hallazgo en este dominio. El asistente lo
  puede señalar como observación sin criterio, pero no hay dónde registrarlo como hallazgo.
- Los datos de la inspección salen hacia un proveedor externo. Se documenta como riesgo de privacidad.

## 2. Diseño

### 2.1 Contexto nuevo `assistance`

Mismo patrón que la capa anticorrupción de Certificación (D16): el asistente tiene su propio modelo de lo que
necesita saber y nunca ve el agregado `Inspection`.

| Pieza | Dónde | Qué es |
|---|---|---|
| `InspectionAssistant` | `assistance.model` | **El puerto que pide la consigna.** `AssistantReply review(InspectionUnderReview)` |
| `InspectionUnderReview` | `assistance.model` | Lo que el asistente puede leer: por criterio, texto, regla en palabras, respuesta, resultado, observaciones y descripciones de evidencia |
| `InspectionReviewTranslator` | puerto en `assistance.model`, impl. en `application.assistance` | Arma `InspectionUnderReview` desde la inspección; único lugar que conoce los dos modelos |
| `AssistantReview` | `assistance.model` | **Agregado.** Id, `InspectionId`, versión de prompt, modelo, quién la pidió y cuándo, y sus `Suggestion` |
| `Suggestion` | `assistance.model` | Criterio, severidad que estimó el modelo, fundamento y citas textuales del texto que la motivó |
| `SuggestionDecision` | `assistance.model` | `PendingDecision`, `Acceptance(stamp)` o `Dismissal(stamp, reason)`: variantes que responden por sí mismas y se describen a un receptor, sin `sealed` ni `switch` (D26) |
| `AssistantReviewService` | `assistance.model` | **Único punto de entrada** a los comandos del agregado, que son de paquete; vuelca la bitácora después de cada uno (D14, D7) |
| `AssistantReviewRepository` | `assistance.repository` | Puerto mínimo: `nextId`, `save`, `findById`, `findByInspection` (D9) |

### 2.2 Guardrails, y dónde vive cada uno

| Regla | Dónde se garantiza |
|---|---|
| La IA nunca cierra, rectifica ni certifica | Estructural: el asistente solo devuelve datos; no tiene referencia a ningún caso de uso ni comando |
| Un humano acepta o descarta | `AssistantReviewService.accept/dismiss`, rol `INSPECTOR`, con `Stamp`; descartar exige motivo |
| Queda trazado qué sugirió la IA y qué decidió el inspector | El agregado guarda la sugerencia completa con prompt y modelo, las rechazadas por guardrail y cada decisión; `AuditLog<ReviewId>`; la inspección enlaza sus revisiones. El acta no cambia: haría que Inspección conozca a Asistencia, al revés del orden de contextos de D30 |
| Solo inspecciones abiertas | Guarda al pedir la revisión y otra vez al guardarla, por si la cerraron mientras respondía el modelo (409) |
| Sugerencia sobre un criterio fuera del alcance | Rechazada por guardrail con su motivo; no se muestra ni se puede aceptar |
| Sugerencia sobre un criterio que ya es no conforme | Rechazada por guardrail: la regla ya levanta ese hallazgo |
| Sugerencia cuyas citas no aparecen en el texto de la inspección | Rechazada por guardrail: es la red principal contra hallazgos inventados |
| Texto de una observación que intenta dar órdenes al modelo (prompt injection) | Inofensivo por diseño: aunque el modelo obedezca, solo puede devolver sugerencias que un humano decide |

**Severidad (cerrada en la spec, §2.4):** el prompt no recibe la severidad configurada de cada
criterio; el modelo estima la gravedad de lo que lee. La pantalla muestra la del criterio, que es la que valdrá si
hay hallazgo, y avisa cuando el modelo estimó otra. Así la métrica "severidad acertada" mide algo real.

### 2.3 Prompts como configuración versionada

- Archivos inmutables en `certiflow-infrastructure/src/main/resources/prompts/inspection-review/v1.st`, `v2.st`…
  Cambiar el prompt es agregar una versión, nunca editar una existente: el mismo criterio que las versiones de
  esquema (F2). Un test con la suma de verificación de cada versión publicada lo hace cumplir (spec §6).
- La versión activa sale de `certiflow.assistant.prompt-version`. Cada `AssistantReview` guarda la versión y el
  modelo con que se generó.
- Responde de paso a la observación general de la entrega 2 (D33): mejorar el asistente es configuración, no código.

### 2.4 Implementaciones del puerto

- `StubInspectionAssistant` (infraestructura): determinista, basado en palabras clave. Lo usan CI, los tests y
  el desarrollo local sin API key.
- `SpringAiInspectionAssistant` (infraestructura): el adaptador real, sobre **Spring AI 2.0**. Usa `ChatClient`,
  carga la plantilla de la versión de prompt activa y convierte la respuesta a un record de Java con salida
  estructurada; una respuesta que no cumple el esquema es un error, no una sugerencia.
- Se elige por `certiflow.assistant.provider=stub|spring-ai`. El perfil `nvidia` compone el cliente compatible con
  NVIDIA NIM (`spring-ai-openai` 2.0.2), con URL, modelo, timeout y key externos. No se usa un starter global:
  la configuración explícita mantiene el stub sin credenciales y evita activar clientes de embeddings, audio o imágenes.

**Decisión: Spring AI en el adaptador, nunca como puerto (2026-11-07).** Spring AI es a los modelos de lenguaje lo
que Spring Data JPA es a la base de datos, y se usa igual: el puerto es nuestro (`InspectionAssistant`, como
`CertificateRepository`) y el framework vive solo en el adaptador de infraestructura (`SpringAiInspectionAssistant`,
como `JpaCertificateRepository`). Lo que aporta:

- **El proveedor pasa a ser configuración.** Cambiar de proveedor es cambiar un *starter* y unas properties, así
  que se puede empezar sin haberlo decidido.
- **El harness compara modelos** con el mismo adaptador, de uno o de distintos proveedores.
- **Salida estructurada y plantillas de prompt desde archivos**, que este plan necesita igual.
- Es compatible con el proyecto: Spring AI 2.0 está hecho para Spring Boot 4.0 y 4.1, y usa Jackson 3.

Lo que no aporta: **no hace el harness**. Sus *evaluators* son de tipo "un modelo juzga a otro"; las métricas de
la consigna (hallazgos correctos, severidades acertadas, hallazgos inventados) las escribimos nosotros.

**Alternativas descartadas:** *el SDK oficial de un proveedor*: más simple y transparente, pero ata el adaptador a
ese proveedor y obliga a escribir otro para comparar modelos en el harness. *HTTP directo con Jackson*: sin
dependencias nuevas, pero hay que escribir a mano el cliente, la salida estructurada y los reintentos de cada
proveedor.

**Costo:** una dependencia más y una segunda capa de abstracción sobre la que ya da el puerto. **Condición:** Spring
AI no sale de infraestructura. ArchUnit lo verifica desde el primer commit, extendiendo la regla de D30 que ya
impide Spring, JPA y Jackson en dominio y aplicación.

### 2.5 Dos decisiones que no existían en las entregas anteriores

- **La llamada al modelo va fuera de la transacción.** `TransactionalUseCases` (D20) envuelve el caso de uso
  entero, y una llamada al modelo tarda segundos: con la base abierta y locks tomados durante ese tiempo. Pedir una
  revisión se arma en tres pasos: leer y traducir la inspección (transacción), llamar al modelo (sin transacción),
  guardar la revisión (transacción). **Cerrada en la spec (§3.4):** un solo caso de uso con un puerto
  `Transactions` que hace explícito el límite, con el mismo principio que `AssetLock` (D27).
- **Que el proveedor falle no es una regla de negocio.** No cae en ninguna de las tres categorías de D28 (404, 400,
  409). **Cerrada en la spec (§3.5):** una cuarta categoría, `UnavailableException`, que la API responde con 503 y
  `problem+json`; la categoría sigue decidiendo el estado, como en D28. Nunca se guarda una revisión a medias.

### 2.6 Reglas de código que se mantienen

Las mismas que la cátedra corrigió en la entrega 1 y que fijó la revisión de calidad
(`docs/specs/2026-10-13-calidad-backend.md` §3.1), aplicadas al código nuevo con el mismo rigor, incluido el harness
aunque no forme parte de la aplicación:

| Regla | Referencia | Cómo se aplica acá |
|---|---|---|
| Dominio y aplicación sin frameworks ni proveedores | D30 | Reglas de ArchUnit nuevas para `assistance` y para Spring AI |
| Agregados con identidad, referencias por id | D3 | `ReviewId`; la revisión conoce la inspección solo por `InspectionId` |
| Comandos del agregado de paquete, alcanzables solo por un servicio de dominio | D14 | `AssistantReviewService` |
| Cada agregado anota sus hechos; bitácora indexada por identidad | D7 | `AuditTrail` en `AssistantReview`, `AuditLog<ReviewId>`; las acciones nuevas se registran en `AuditActionCatalog` desde la composición (D2) |
| Quién y cuándo como hecho de dominio | D12 | `Stamp` en el pedido y en cada decisión |
| Un `XxxCommand` por caso de uso de escritura, con ids y códigos | D17 | `RequestAssistantReviewCommand`, `AcceptSuggestionCommand`, `DismissSuggestionCommand` |
| Puertos segregados | D9 | El repositorio expone solo lo que usan los casos de uso |
| Variantes que responden en lugar de `null`; sin `sealed`, `instanceof` ni `switch` por tipo | D26 | Estado de la decisión, respuesta del modelo válida o inválida, sugerencia con o sin citas |
| `Optional` solo como retorno; `if` solo como guarda que lanza | Calidad §3.1, D8 | Sin condicionales disfrazados para esquivar un `if` |
| Excepciones propias en una de tres categorías | D11, D28 | Revisión inexistente (404), inspección cerrada o sugerencia ya decidida (409), motivo vacío (400) |
| El tiempo llega por un `Clock` inyectado, leído una vez por operación | D29 | En los dos casos de uso de escritura |
| Sin estáticos ni clases utilitarias | Calidad §3.1 | El cargador de prompts, las métricas y el lector del conjunto dorado son objetos con colaboradores |
| Value objects en lugar de primitivos | Calidad §5.13 | `PromptVersion`, `ModelId`, `ReviewId`; la sugerencia usa `CriterionCode` |
| Persistencia con snapshots y entidades JPA propias | D18, D19 | Snapshot de la revisión; las sugerencias, inmutables, en JSON |
| Una migración nueva por cambio; nunca editar una ya aplicada | lección de V1 | `V7__assistant_reviews.sql` |
| REST nivel 3, DTOs propios, nunca tipos del dominio en la respuesta | D21, D25 | Recursos y links de §Fase 4; `@AllowedRole` y `@ReadableBy` |
| El frontend no deduce acciones del estado | D23, D24 | Botones según los rels de la respuesta; `roles.ts` |
| Sin código que solo usan los tests | Calidad §5.13 | Revisarlo antes de cerrar cada fase |
| Formato | Calidad §3.1 | Nombres que se explican solos, una sentencia por línea, sin `var`, sin imports con comodín, líneas de hasta 150 caracteres |
| Mocks solo en tests unitarios | Consigna E2, D23 | Casos de uso con el puerto simulado; repositorio y API contra la base real |

## 3. Harness de evaluación

**Qué es y por qué:** el asistente no es determinista; la misma entrada puede dar salidas distintas, así que un
`assertEquals` no lo prueba. El harness corre un conjunto dorado varias veces y mide tasas, y lo que se exige es
que una versión de prompt no empeore las métricas respecto de la anterior.

- **Módulo** `certiflow-harness`, que depende de dominio e infraestructura y no forma parte de la aplicación.
- **Conjunto dorado** en `certiflow-harness/src/main/resources/golden/case-NN.json`, 15 casos sobre los esquemas de la demo (matafuego y caldera con
  subsistemas):
  - unos 10 con no conformidades escondidas en observaciones o evidencia, con criterio y severidad esperados;
  - 3 inspecciones limpias, para medir hallazgos inventados;
  - 2 trampas: negaciones ("no se observan fugas") y redacción ambigua.
- **Métricas por caso y totales:** hallazgos correctos (criterio esperado sugerido), severidades acertadas (entre
  los correctos), hallazgos inventados (sugeridos y no esperados). Cada caso corre 5 veces; se reporta media y
  dispersión.
- **Salida:** `harness/results/<versión de prompt>/<modelo>.json` y un resumen en Markdown, versionados en el repo
  para que el cambio de un prompt muestre su comparación en el pull request.
- **CI determinista:** un test del módulo corre el harness contra el stub y verifica que las métricas se calculen
  bien sobre salidas conocidas.
- **Modelo real, por separado:** workflow `harness.yml`, disparado al cambiar los prompts y a mano, con la API key
  como secreto. Corre el harness, compara con la versión anterior y publica el resultado.

## 4. Fases y tareas

### Fase 0 · Spec y conjunto dorado
- [x] Copiar la consigna a `docs/consigna/certiflow-entrega-3.md`.
- [x] Spec [`docs/specs/2026-11-07-asistente-ia.md`](../specs/2026-11-07-asistente-ia.md): contrato del puerto,
      formato de entrada y salida, guardrails, endpoints, matriz riesgo → test y definición de terminado. Cierra la
      severidad (§2.4), las transacciones (§3.4) y la respuesta a una falla del proveedor (§3.5).
- [x] Redactar los 15 casos del conjunto dorado
      ([`certiflow-harness/src/main/resources/golden`](../../certiflow-harness/src/main/resources/golden/README.md)):
      10 escondidos, 3 limpios y 2 trampas sobre los esquemas de la demo, validados contra sus reglas.
- [ ] Revisarlos en equipo: tienen que sonar a inspecciones reales (guía en el README del conjunto).

### Fase 1 · Dominio
- [x] Contexto `assistance`: puerto, `InspectionUnderReview`, `AssistantReview`, `Suggestion`, decisiones,
      `ReviewId`, excepciones (revisión de inspección cerrada, sugerencia ya decidida, índice inexistente).
- [x] Guardrails de §2.2 en el agregado y en su fábrica, con tests unitarios parametrizados.
- [x] Regla de ArchUnit: `assistance` no importa el modelo de inspección salvo ids, y nadie fuera de infraestructura
      importa el proveedor.

### Fase 2 · Aplicación
- [x] `RequestAssistantReviewUseCase`: carga la inspección y la traduce, llama al puerto **fuera de la
      transacción**, aplica los guardrails, guarda y audita (§2.5).
- [x] Aceptar y descartar, con autor y momento, auditados: dos casos de uso, `AcceptSuggestionUseCase` y
      `DismissSuggestionUseCase`, uno por comando (D17), como fija la spec.
- [x] Consultas: revisiones de una inspección, una revisión por id y su bitácora.
- [x] `InspectionReviewTranslatorImpl` y sus tests; tests de casos de uso con dobles de los puertos.

### Fase 3 · Infraestructura
- [x] Migración `V7__assistant_reviews.sql`, entidades, mapper y repositorio JPA, con tests de repositorio sobre
      H2. Sobre PostgreSQL la migración corre en CI, que ahora también dispara en `entrega-3*`.
- [x] `StubInspectionAssistant` y carga de prompts versionados (`PromptCatalog`, con suma de verificación).
- [x] Dependencia de Spring AI 2.0 (`spring-ai-client-chat` 2.0.2) solo en `certiflow-infrastructure` y regla de
      ArchUnit que lo impide fuera de ella.
- [x] `SpringAiInspectionAssistant` con salida estructurada validada y error explícito si el proveedor falla o
      responde algo que no cumple el formato, probado con un `ChatModel` falso.
- [x] Proveedor seleccionado: NVIDIA NIM, configurado con `spring-ai-openai` y composición explícita en
      infraestructura (en lugar de un starter global). Perfil `nvidia`, key por entorno, modelo y timeout
      configurables; selección `certiflow.assistant.provider=stub|spring-ai`, sin fallback silencioso.
- [x] API key aportada por el usuario para las pruebas, sin guardarla en el repositorio.
- [ ] Comprobar la primera revisión real y registrar su resultado. Las pruebas locales del cliente usan
      un servidor HTTP; no reemplazan esta verificación externa.

Verificación de la composición NVIDIA: `mvn -q clean verify` pasó; cinco pruebas nuevas del cliente
HTTP local, más las regresiones y ArchUnit. Esa suite no llama a NVIDIA ni prueba calidad del modelo;
los intentos externos posteriores fallaron (ver traspaso).


### Fase 4 · API implementada; integración real pendiente
- [x] `POST /api/v1/inspections/{id}/assistant-reviews` (201), `GET` de colección e ítem, y
      dos transiciones como recursos propios, igual que `closure` o `cancellation` (D21):
      `POST …/assistant-reviews/{n}/suggestions/{k}/acceptance` y `POST …/suggestions/{k}/dismissal` con
      `{"reason"}`. No un `{"accepted": true|false}`: sería un booleano que elige comportamiento.
- [x] HAL: la inspección abierta ofrece `assistant-reviews`; una sugerencia pendiente ofrece `acceptance` y
      `dismissal`, y una ya decidida ninguno de los dos.
- [x] Rol `INSPECTOR` para pedir y decidir; problem+json; `docs/openapi.yaml`; tests de API y de roles.

**Implementada y probada con stub el 2026-11-08; NO validada de extremo a extremo con NVIDIA.** Las decisiones creadas tienen GET en su
`Location`. El link `assistant-reviews` permanece al cerrar la inspección para consultar la traza; pedir otra
revisión cerrada sigue devolviendo 409. Aceptar y descartar solo trazan decisiones, incluso después del cierre.
Tests: `AssistantReviewApiTest`, `AssistantUnavailableApiTest`, roles y `PostgresAssistantReviewApiTest`.
La composición NVIDIA de fase 3 se agregó después. Se aportó una key temporal, pero no se guardó
y ninguna inferencia tuvo éxito; la primera revisión real sigue pendiente (§5.4).
Verificación local: `mvn -q -Dcertiflow.postgres=true clean verify` pasó sobre H2 y PostgreSQL 17;
OpenAPI validado como YAML sin claves duplicadas y con todas sus referencias internas resueltas.

### Fase 5 · Harness
- [ ] Módulo `certiflow-harness`: carga del conjunto dorado, ejecución repetida, métricas, reporte y comparación
      entre versiones.
- [ ] Test de CI contra el stub; workflow `harness.yml` con el modelo real.
- [ ] Primera corrida real con `v1`; escribir un `v2` que ataque los errores encontrados y comparar. Esa
      comparación es material de la presentación.

### Fase 6 · Frontend
- [ ] Puerto y adaptador HTTP del asistente, con su mapeo probado contra respuestas reales guardadas.
- [ ] Panel "Revisar con IA" en `InspectionDetailPage`: cada sugerencia con criterio, severidad del criterio y la
      estimada, fundamento y citas; aceptar lleva al criterio para corregir la respuesta; descartar pide motivo.
- [ ] Marca visible de lo sugerido por IA y lo decidido por el inspector; tests de pantalla y e2e contra el stub.

### Fase 7 · Documentación
- [ ] `DESIGN.md`: decisiones nuevas para el puerto y el contexto, guardrails, prompts versionados, harness y CI;
      por qué esta funcionalidad y no el acta; patrones no aplicados; tabla riesgo → test; limitaciones.
- [ ] `README.md` (cómo correr con stub o con el modelo real), `CHANGES.md` y diagrama PlantUML.

### Fase 8 · Video de presentación

Video de 8 a 12 minutos; hablan los cinco, cada uno de una parte distinta. Cada parte presenta **qué se decidió,
qué alternativas se exploraron y por qué se eligió esa**. Reparto propuesto, a unos 2 minutos por persona:

| # | Parte | Contenido | Fuente |
|---|---|---|---|
| 1 | Dominio desde cero | El problema, los actores (seis roles), las reglas centrales: versión capturada al asignar, cierre inalterable y rectificación auditable, hallazgos derivados de las reglas, separación de responsabilidades | DESIGN §1 (supuestos) y glosario |
| 2 | Decisiones de dominio | Agregados con identidad, Strategy para reglas y políticas, contextos y capa anticorrupción, y cómo F1, F2 y F3 se apoyaron en eso; con sus alternativas descartadas | DESIGN §2 y §3 (D1, D3, D10, D16, D31, D32) |
| 3 | Endpoints | REST nivel 3: recursos sin verbos, una transición es un recurso nuevo, HAL con links según el estado, roles, problem+json; alternativas (RPC, un DTO por agregado, springdoc); mostrar `docs/openapi.yaml` y una navegación desde `GET /api/v1` | D21, D25, `docs/openapi.yaml` |
| 4 | Refactors entre entregas | Entrega 1 → 2 → 3: un módulo → cuatro módulos DDD por contexto; decoradores de auditoría → bitácora en cada agregado; `Object` → `AuditLog<S>`; referencias por objeto → por id; persistencia con snapshots; revisión de calidad (variantes en lugar de `null`); correcciones de las devoluciones. Para cada uno, qué lo motivó | `CHANGES.md`, `Corrections.md`, historial de git |
| 5 | Propuesta de valor | Por qué aplica y en qué forma (§1), integración detrás del puerto, guardrails, demo grabada, qué es un harness y por qué no alcanza con tests de igualdad, resultados del conjunto dorado, `v1` contra `v2` y errores reales del asistente | §1 a §3 de este plan |

La propuesta de valor es la parte más densa: si el tiempo no alcanza, le cede minutos el resto, no al revés.

- [ ] Guion por parte, con tiempos, y una lámina de "alternativas → elegida → por qué" en cada una.
- [ ] Demo grabada aparte para insertar en la parte 5: inspección con una no conformidad escondida en una
      observación → revisión → aceptar → corregir la respuesta → cerrar → el hallazgo aparece y la traza muestra quién
      decidió qué. Al ser grabada, se puede repetir hasta que salga limpia.
- [ ] Lámina del harness con números reales de la primera corrida y de la comparación entre versiones.
- [ ] Grabación (una llamada de Meet grabada alcanza), con los cinco en cámara en su parte.
- [ ] Edición mínima y control de duración: entre 8 y 12 minutos, ni uno de más ni de menos.
- [ ] Subir a YouTube como **no listado** y verificar que el enlace abre sin iniciar sesión.

### Fase 9 · Entrega
- [ ] Todo entra a `main` por pull request, que exige los tres checks de CI. Commits en inglés, en imperativo y con
      el porqué en el cuerpo, como el resto del historial; cada fase deja `DESIGN.md` y `CHANGES.md` al día en su
      mismo pull request, no al final.
- [ ] Último commit de la entrega en `main`, con CI verde.
- [ ] Enviar los dos enlaces: repositorio con el hash de ese commit, y el video.

## 5. Riesgos y pendientes

1. **El video es condición para que se evalúe el TPI.** Hay que dejar margen para grabar, editar y subir; no
   dejarlo para el último día. Si falta alguien en la grabación, falta un requisito.
2. **Duración.** Cuatro temas, cada uno con alternativas, en 8 a 12 minutos: el guion tiene que estar cronometrado
   antes de grabar, no ajustarse en la edición.
3. **Latencia y costo.** Una revisión es una llamada de pocos segundos; el harness completo, unas decenas de
   llamadas. Se documentan los números reales después de la primera corrida.
4. **Proveedor NVIDIA; modelo corregido (2026-11-08).** Los endpoints gratuitos de los Llama inicialmente
   elegidos están retirados. Se usa `google/gemma-4-31b-it`, cuyo catálogo muestra endpoint gratuito activo.
   La comparación pequeña queda pendiente de comprobar disponibilidad actual. Se desactiva el razonamiento
   opcional de Gemma mediante `chat_template_kwargs.enable_thinking=false`, configurable en el perfil.
   El usuario aportó una key para probar la conexión; solo se usa en memoria y no se registra aquí.
   No hay resultados del harness completo todavía. La primera consulta HTTP agotó el timeout de 45 segundos;
   no se considera una validación de credencial ni de calidad. El adaptador Java también agotó 120 segundos;
   una consulta mínima de 32 tokens devolvió 504 con una ventana de polling de 5 segundos. El listado de modelos
   es público y no valida la key. Evidencia sin secretos: `docs/validation/2026-11-08-nvidia-gemma-connectivity.json`.
   La primera generación, los guardrails externos y el harness siguen sin validar.
   Fuentes: https://build.nvidia.com/google/gemma-4-31b-it y
   https://docs.api.nvidia.com/nim/reference/google-gemma-4-31b-it-infer .

## 6. Definición de terminado

Cada requisito de la consigna tiene evidencia en el repo:

| Requisito | Evidencia |
|---|---|
| Funcionalidad asistida | Revisión antes del cierre, de punta a punta: API, pantalla y tests |
| Guardrails | Tests de dominio y de API; traza visible en la API, la pantalla y el acta |
| Puerto propio | `InspectionAssistant` en el dominio; ArchUnit verde |
| Harness | Conjunto dorado de 15 casos, ejecutable, con las tres métricas |
| Versionado de prompts | `prompts/…/v1`, `v2`; resultados comparados en el repo |
| CI determinista | CI verde usando solo el stub; workflow aparte para el modelo real |
| Tests | Unitarios, de repositorio, de API y del harness, en CI |
| DESIGN.md | Decisiones nuevas con alternativas y consecuencias |
| Video | 8 a 12 minutos en YouTube no listado; los cinco hablan de partes distintas; cubre decisiones de dominio, endpoints, refactors y propuesta de valor, cada uno con alternativas y el porqué |
| Entrega | Enlace al repositorio con el hash del commit final y enlace al video |
