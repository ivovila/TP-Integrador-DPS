# Calidad de código del backend · Plan de implementación

**Goal:** Que el backend cumpla las reglas de diseño del equipo en sus cuatro módulos, sin cambiar lo que hace: sin `sealed`, sin preguntas por el tipo, sin `null` ni estáticos en dominio y aplicación, y con las reglas de negocio en el dominio.

**Architecture:** La misma. Cinco mecanismos resuelven casi todo (spec §4): el objeto responde, la variante se describe a un receptor, la ausencia es una variante, la lectura selecciona con `appliesTo` y `findFirst`, y los estáticos pasan a objetos con colaboradores.

**Tech Stack:** el actual (Java 25, Spring Boot 4.1.1, JPA, Flyway, PostgreSQL). Única dependencia nueva, a confirmar: ArchUnit, solo para tests.

**Spec:** [docs/specs/2026-10-13-calidad-backend.md](../specs/2026-10-13-calidad-backend.md) · **Inventario:** [docs/specs/2026-10-13-calidad-backend-inventario.md](../specs/2026-10-13-calidad-backend-inventario.md)

## Global Constraints

- Es una refactorización: ningún test existente cambia lo que comprueba. Si un test hay que tocarlo, es porque cambió una firma, y sigue probando el mismo comportamiento.
- Sin migraciones. Las columnas y los JSON guardados conservan forma y valores (spec §6).
- El contrato HTTP cambia solo en lo que lista la spec §6. Rutas, `rel` y códigos de estado no cambian.
- `certiflow-domain` y `certiflow-application` siguen sin importar Spring, JPA ni Jackson.
- D8 sigue vigente: ninguna tarea reemplaza un `if` por una tabla de predicados o un enum hecho para esquivarlo.
- Ningún método de un receptor tiene implementación por defecto ni lanza.
- En `certiflow-web` solo se edita lo que deja de compilar por un cambio de los otros módulos, y lo editado produce el mismo JSON. Lo demás del módulo web se anota al final, en «Pendiente del módulo web».
- Antes de cada tarea: `git fetch` y actualizar. La rama recibe commits de todo el equipo y varias tareas tocan archivos que otros están editando.
- Después de cada tarea: `mvn -q clean verify`, `npm run test:e2e` contra el backend levantado y el inventario regenerado. Los tests se verifican en un paso aparte, antes del commit.
- Un commit por tarea, mensaje en inglés y en imperativo, sin `Co-Authored-By` ni atribución.
- Nombres largos y explícitos; los de la spec §10 son propuestas hasta que el equipo los confirme. Sin comentarios explicativos en código nuevo.

## Review Focus

1. **Ningún `null` sale de un espejo.** Lo que Jackson o JPA entregan como `null` se convierte en una variante dentro del mismo record o entidad, y ninguna firma del dominio o de aplicación lo recibe.
2. **Lo guardado se lee igual.** Cada cosa que hoy se distingue con `instanceof` tiene un test de ida y vuelta, y las filas escritas por el código anterior se siguen leyendo.
3. **Las respuestas de la API dicen lo mismo.** Mismos motivos de rechazo, mismos estados HTTP, mismos links en cada estado.
4. **No aparece un condicional disfrazado.** Si una solución termina en un mapa de etiqueta a función o en una lista de predicados, está mal resuelta.
5. **`shared` no importa ningún contexto** al terminar la tarea 8.

## Mapa de archivos

| Dónde | Qué |
|---|---|
| `certiflow-domain` · `certification` | `CertificationDecision` y sus variantes, `ValidityTerm`, `CertificationScope`, `CertificateBasis`, `Certificate`, `InspectionResult`, `OpenNonConformity`, `Requirement`, políticas, `CertificationEligibility`, `AssetCertificates`, comandos |
| `certiflow-domain` · `schema`, `inspection`, `catalog` | `RawAnswer`, `Answer`, las tres reglas, `Criterion`, `Section`, `SchemaVersion`, `SchemaSections`, `CriterionResponse`, `Finding`, `CorrectiveAction`, `InspectionScope`, snapshots, `SubsystemAssociation`, catálogos, comandos |
| `certiflow-domain` · `shared` | categorías de `DomainException`, `AuditAction`; se van `AuditSubject`, `AuditLogs`, `AssetLockKey`, `LocksAsset`, `StoredInstant` |
| `certiflow-application` | servicios de certificación, los ocho casos de uso que bloquean, casos de uso de historial, consultas con filtro |
| `certiflow-infrastructure` | entidades y embebibles, las siete utilitarias, `CertificationTransactionLocks`, repositorios JPA |
| `certiflow-web` | controllers, recursos y payloads, ensambladores, `ApiExceptionHandler`, configuración por contexto, `DemoDataSeeder`, `web.security` |
| `certiflow-frontend` | solo tarea 12: `wire.ts`, tres mapeos, `__fixtures__/` |
| raíz | `DESIGN.md`, `docs/certiflow-domain.puml`, `README.md`, `CHANGES.md`, `docs/specs/README.md` |

---

### Task 0: Base en verde

- [x] El job `e2e` de CI falla en `c7d4b7a` porque su sonda entra con un id de persona y el login ya es por nombre de usuario. Se corrige la sonda.
- [x] Decisiones confirmadas el 2026-10-13 (spec §9.1). **Alcance: dominio, aplicación e infraestructura.** Las tareas 10, 11 y 12 quedan para el plan del módulo web.

### Task 1: Decisión de certificación y plazos (spec §5.1)

- [x] `CertificationDecision` como interfaz con `GrantedDecision` y `RejectedDecision`; `Certificate` recibe un `GrantedDecision` y deja de guardar `scope` y `validity`.
- [x] `MonthsTerm`, `DaysTerm` y `ConditionalTreatment`; se va `ValidityTerm.notGranted()`.
- [x] Los enums de ciclo de vida y estado responden en lugar de compararse.
- [x] `DecisionJson` y los payloads de decisión escriben el mismo JSON. Test de ida y vuelta.

### Task 2: Alcance, base y alcance de inspección (spec §5.2)

- [x] `CertificationScope` y `CertificateBasis` sin `sealed`, con su comportamiento y su receptor; `InspectionScope` con su receptor.
- [x] Receptores en `CertificateEntity`, `InspectionEntity`, `DecisionJson`, payloads y links; lectura de `scope_type` y `basis_type` por selección.
- [x] Se borran `Certificate.basedOn()` y `inspectionId` del recurso; el alcance pedido al renovar es una variante.
- [x] Verificación: secciones 1, 2 y 3 del inventario en cero, salvo excepciones y seguridad.

### Task 3: Respuestas (spec §5.3)

- [x] `RawAnswer` con texto; cada regla interpreta el suyo; `RawAnswerTest` con los casos nuevos.
- [x] `Answer.describedBy`; receptores en `AnswerColumns`, `EvaluationJson` y los dos recursos de la API.
- [x] Se van el registro de `AnswerColumns` y la tabla de `RawAnswers`.

### Task 4: Ausencias del dominio (spec §5.4)

- [x] `SubsystemAssociation` en criterio, sección, hallazgo, línea y no conformidad.
- [x] `NotAnswered`, `NotCancelled`, `StillOpen` y `EffectiveOnPublication` con sus pares; `InspectionResult` sin excepciones del contexto de inspección.
- [x] Verificación: cero `null` y cero `Optional.ofNullable` en dominio y aplicación.

### Task 5: Reglas, catálogos y acciones de bitácora (spec §5.5)

- [x] `ApprovalRule.describedBy`; en persistencia, `RuleDocumentDescriber` para escribir y un `StoredRuleReader` por tipo de regla para leer. (Reemplazado en la Task 15 por `RuleDescriber` y `ApprovalRuleKind`.)
- [x] `SeverityCatalog`, `EvidenceKindCatalog` y `AuditActionCatalog`, armados en la composición. `IneligibilityReasonCatalog` no hace falta: un certificado solo guarda una decisión otorgada, que no tiene motivos de rechazo.
- [x] `AuditAction` con `name()` y `code()`; se va `AuditActions` de persistencia con sus casts.
- [ ] Diferido al plan del módulo web: un payload por tipo de regla y lectores por tipo en `SchemaPayloads`, los catálogos en `VocabularyController` y el cast a `Enum` de `AuditEntryResponse`.

### Task 6: Excepciones (spec §5.6)

- [x] Tres categorías bajo `DomainException`; cada excepción extiende la suya.
- [x] `ApiExceptionHandler` con un método por categoría. Los tests de API que ya fijaban estados siguen pasando sin cambios en el primer commit.
- [x] Excepciones propias en lugar de `Illegal*`, de `orElseThrow()` sin argumento y de los textos libres.
- [x] Primer commit: las excepciones que respondían 409 por descarte van a la categoría que responde 409. Ningún estado HTTP cambia.
- [x] Segundo commit: reclasificación. 404 cuando un identificador del request no nombra nada; 400 cuando un valor está mal por sí mismo; 409 cuando el estado de un agregado que existe impide la operación. Pasan de 409 a 400: `AssignerCannotInspectException`, `DueDateInThePastException`, `InvalidValidityPeriodException`, `RectificationReasonRequiredException` y `VerificationNotesRequiredException`. Ninguna pasa a 404. El frontend no lee el estado de un error de dominio (solo su título), así que no cambia.
- [ ] Diferido al plan del módulo web: las excepciones de dominio que el módulo web lanza para decir 400 o 404 (`RequestFields`, `SchemaPayloads`, `RawAnswers`, las búsquedas dentro de agregados en dos controladores). El test que impide extender `DomainException` directamente se escribe con ArchUnit en la Task 14.

### Task 7: Reglas de negocio y estáticos del dominio (spec §5.7)

- [x] `CertificationEligibility` y `AssetCertificates` en el dominio; los servicios de aplicación delegan.
- [x] `Requirement` y `Requirements`; políticas de ejemplo construidas en `CertificationConfiguration`.
- [x] `SchemaSections(List<Section> all)`, con los subsistemas requeridos derivados de los criterios y `ensureRequires` para lo que declara quien publica; constructores en lugar de `none()` y `startingWith(…)`.
- [x] `Clock` con la precisión guardada y truncado de los instantes del cliente en el request; se va `StoredInstant`.
- [x] Puerto de `catalog` para jurisdicciones conocidas; `RegisterAssetUseCaseImpl` deja de importar `certification`.
- [x] Verificación: cero estáticos en dominio y aplicación.

### Task 8: Bloqueo por activo y bitácora (spec §5.8)

- [x] `assetCodeOf(…)` en los repositorios de inspección, hallazgo y certificado.
- [x] Los ocho casos de uso que ya bloqueaban (doce desde la Task 15) bloquean con `AssetLock`, en el orden resolver el código, bloquear y cargar; se van `LocksAsset`, `AssetLockKey` y `CertificationTransactionLocks`.
- [x] Un caso de uso de historial por contexto; se van `AuditSubject` y `AuditLogs`.
- [x] Los tests de concurrencia de `DeliveryFeaturesApiTest` siguen pasando.
- [x] `RenewCertificateUseCaseImpl` resuelve el activo desde el certificado que se renueva y no desde la inspección: así, si faltan los dos, el error sigue siendo el del certificado, como hoy. Los tests de concurrencia pasan también contra PostgreSQL real (`PostgresFeatureApiTest`).

### Task 9: Infraestructura (spec §5.9)

**Orden real: se hizo antes de la Task 5.** Los catálogos y los lectores de reglas de la Task 5 necesitan un objeto donde inyectarse, y las utilitarias estáticas no lo ofrecen. Quedan para más adelante lo que depende de otras tareas: `AuditActions` (Task 5), `Instants` (Task 7) y `CertificationTransactionLocks` (Task 8).

- [x] Las utilitarias como objetos; un solo `JsonMapper`; constructores en lugar de `from` y `of`.
- [x] Fusiones posicionales sin condición; `JpaInspectionRepository` sin la clase concreta de esquemas.
- [x] Se borra el código sin uso.

### Task 10: Web (spec §5.10) · diferida al plan del módulo web

- [ ] Transiciones disponibles en las vistas del dominio y un receptor de links por recurso.
- [ ] Un ensamblador por recurso; ningún controller depende de otro ni llama a otro handler.
- [ ] Links por ruta; las utilitarias como objetos inyectados.
- [ ] Filtros y página en los casos de uso; lo que depende del reloj vuelve resuelto.
- [ ] Los records de request validan y convierten sus campos; se va `RequestFields`. `DemoDataSeeder` en su paquete.

### Task 11: Seguridad (spec §5.11) · diferida al plan del módulo web

- [ ] Solo si se confirma en la tarea 0. Un mismo principal para Basic y para el token; emisión del token en el punto de extensión de Basic; revocación declarada en la cadena de seguridad.
- [ ] Cuentas y tokens revocados detrás de puertos, con el caso de uso `RevokeToken`.

### Task 12: Variantes en las respuestas y frontend (spec §6) · diferida al plan del módulo web

- [ ] Solo si se confirma en la tarea 0. `sections`, la respuesta dada y la vigencia de la decisión dejan de venir en `null`.
- [ ] `wire.ts`, `inspectionMapping.ts`, `certificationMapping.ts`; volver a capturar las respuestas de ejemplo.
- [ ] Verificación: tests rápidos del frontend y `npm run test:e2e`.

### Task 13: Nombres, formato y código sin uso (spec §5.13)

- [x] Renombres con la convención del equipo: `Stamp(author, instant)`, `recordAuditEntriesOf`, `publishVersion`, `RevisionEmbeddable`, `ReplacementForAnotherAssetException`.
- [x] `CriterionCode`, `SectionTitle` y `PolicyCode`: la persistencia y la API escriben `.value()`, así que lo guardado y el JSON no cambian.
- [x] `ActionPlan` con su regla; `FindingsSummary.openFindings()` derivado.
- [x] En dominio, aplicación e infraestructura: sin `var`, sin líneas de más de 150 caracteres y sin cuerpos en una línea.
- [x] Se borra lo que solo llamaban los tests (`Evaluation.outcomeOf`, `Characteristics.valueOf`, `Revisions.size`, `InspectionSchema.appliesTo`), y los tests leen lo mismo por el comportamiento público.
- [ ] Diferido al plan del módulo web: los nombres, los imports con comodín, las líneas largas y los cuerpos en una línea de `certiflow-web`.

### Task 14: Documentación y regla ejecutable

- [x] `DESIGN.md`: decisiones nuevas (D26 a D30), D17 reemplazada, D18 con su riesgo, D21 actualizada, fronteras y pendientes en limitaciones, y las líneas desactualizadas.
- [x] `docs/certiflow-domain.puml`, `README.md`, `CHANGES.md`.
- [x] Tests de arquitectura con ArchUnit (`ArchitectureRulesTest`): capas, `shared` sin contextos, orden entre contextos, dominio y aplicación sin frameworks, certificación sin el modelo de inspección salvo sus ids, y toda excepción de dominio dentro de una categoría.
- [x] Lista «Pendiente del módulo web», abajo.
- [x] Inventario final regenerado.

### Task 15: Respuesta a la auditoría externa de `3335b26`

Una auditoría sobre el commit publicado y dos observaciones de diseño. Se evaluó cada punto contra el código; lo que sigue es lo que se corrigió. Los hallazgos del módulo web (el `Location` de un sub-recurso, `SchemaPayloads` por clase, los nombres de clase como contrato de errores) y los del frontend y de seguridad quedan en la lista de abajo.

- [x] **Escritura atrasada sobre una inspección cerrada.** Responder, adjuntar evidencia, observar y planificar una acción bloquean el activo antes de leer, como los otros ocho. Tests que retienen el activo en otra transacción.
- [x] **Altas simultáneas que se pisaban.** `AssetRepository.add` e `InspectionSchemaRepository.add` insertan de verdad; el segundo pedido recibe la excepción de dominio (409).
- [x] **Métodos donde una variante respondía "yo no".** `CertificationDecision` pierde `conditions()`, `ineligibilities()` y `granted()`; `ensureGranted()`, `ensureBacksGlobalCertificate(...)` y `referenceOf(...)` son guardas. El payload de la decisión se arma con el receptor, con el mismo JSON.
- [x] **Una regla nueva obligaba a tocar el core.** `RuleDescriber` por clase de valor, `ApprovalRuleKind` y `ApprovalRuleKindCatalog`; se van `ApprovalRuleDescriber`, `RuleDocument` y los tres `StoredRuleReader`. Una regla guardada lleva solo sus parámetros; las filas anteriores se siguen leyendo.

---

## Pendiente del módulo web

Lo que no se tocó en `certiflow-web`, para el plan propio de ese módulo. Las secciones citadas son las de la spec.

**Respuestas con `null` (spec §6; exige adaptar el frontend).**
- `InspectionResponse.sections` cuando el alcance es completo.
- `CriterionResponseResource`: `answer`, `answeredBy` y `answeredAt` mientras el criterio está pendiente. Cuando se resuelva, `CriterionResponse.given()` puede irse del dominio: hoy existe porque este recurso lo consume.
- `DecisionPayload`: `validFrom` y `validUntil` en una decisión rechazada.
- `ScopePayload`, `BasisPayload` y `RulePayload`: un único record con los campos de todas las variantes, vacíos los que no son de la suya.
- `CertificateResource.inspectionId`, que repite la base.

**Condicionales y tipos (spec §5.10).**
- Los `if` que deciden links según el estado (inspección abierta o cerrada, certificado activo, hallazgo abierto, páginas `prev` y `next`).
- `methodOn(..., null)` para armar links.
- `SchemaPayloads`: registros por clase y por texto para las reglas, cuatro casts, bloque `static`; severidades y tipos de evidencia resueltos contra los enums en lugar de los catálogos (también en `VocabularyController`).
- `AuditEntryResponse`: cast a `Enum` para leer el nombre de la acción; `AuditAction.name()` ya existe.
- `CriterionResponseController` y `FindingController` buscan dentro de un agregado y lanzan excepciones de dominio; la búsqueda es del dominio.

**Excepciones (spec §5.6).**
- `RequestFields`, `AssignmentRequest`, `VerificationRequest`, `SchemaPayloads` y `RawAnswers` lanzan excepciones de dominio (`RequiredValueMissingException`, `InvalidRuleConfigurationException`, `AnswerTypeMismatchException`) para decir 400; les corresponde `InvalidRequestException`.
- `PageLinks` repite la validación de `PageRequest` con su propia excepción.

**Estáticos y utilitarios (spec §5.10).**
- `RawAnswers`, `RequestFields`, `PageLinks`, `Uris`, `SchemaPayloads`, `CertificationPayloads`, `TransactionalUseCases` (este último se deja como está por decisión).
- `DemoDataSeeder`.
- Los veintiséis `TransactionalUseCases.wrap(...)` repetidos en las clases de configuración y el parámetro `tx`.

**Otros.**
- Parámetros de consulta opcionales manejados con `null` y filtros aplicados en el controlador.
- Paginación en memoria.
- El número de un sub-recurso nuevo se deduce de un tamaño después de una segunda lectura.
- Controladores que dependen de otros controladores.
- Nombres (`GlobalRequest`, campos de `GlobalCertificateController`, lambdas de una letra), dos imports con comodín, líneas de más de 150 caracteres y cuerpos en una línea.
- `RequestFields.requireElements` llama a `contains(null)`, que lanza `NullPointerException` sobre una lista inmutable como `List.of()`. Con las listas que arma Jackson no ocurre; apareció al escribir `VersionRequestTest`.
- Seguridad (spec §5.11): fuera de alcance por decisión; lo encontrado en ese código está listado en la spec, §5.11.

**De la auditoría externa de `3335b26` (Task 15).**
- `SchemaPayloads` sigue eligiendo por clase al publicar una regla y por texto al recibirla: una regla nueva todavía obliga a tocarlo. Le corresponde implementar `RuleDescriber` y `RuleParameters` sobre el JSON de la API, y buscar el tipo en `ApprovalRuleKindCatalog`, como ya hace la persistencia.
- `AnswerDescriber` tiene un método por tipo de respuesta. Sus tres métodos ya son, de hecho, clases de valor (un número, un booleano, un texto); renombrarlos así deja que una respuesta nueva use uno existente. `AnswerPayloadDescriber` depende del tipo JSON, por eso va con este módulo.
- El `Location` de una observación, una evidencia, una revisión o una verificación recién creada se deduce de un tamaño después de una segunda lectura: con dos pedidos simultáneos puede ser el del otro. El caso de uso tiene que devolver el número que creó.
- `ProblemDetails` usa el nombre de la clase de la excepción como título y tipo del problema, y el frontend mantiene la lista de esos nombres: renombrar una excepción cambia el contrato. Hace falta un código estable por problema.
- `TokenController` construye entidades JPA y maneja la transacción de la revocación.
- Frontend: `actionsOfferedBy` conserva el `rel` y descarta el `href`, y `HttpInspectionPort` arma las rutas a mano, contra lo que dice D23.
