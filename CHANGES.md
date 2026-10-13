# Cambios a partir de las correcciones (Entrega 1)

Registro de lo que se modificó en respuesta a `Corrections.md`, en el orden en que se resolvió.

---

## 16. Segunda revisión: huecos que quedaron abiertos

Revisión posterior del código corregido. Las correcciones 1 a 15 habían quedado a medias en estos puntos.

**16.1. Un hallazgo cerrado tapaba una no conformidad reintroducida al rectificar.** `Finding.raisedFrom`
comparaba inspección y criterio sin mirar el estado del hallazgo, así que, después de cerrar una acción
correctiva, una rectificación que volvía a rechazar el mismo criterio salía certificada.
- Rectificar ahora levanta hallazgo: `Inspection.rectify` devuelve `InspectionRectified` con las no
  conformidades que la respuesta nueva introduce, y `FindingService.raiseFrom` levanta uno por cada una que no
  tenga ya un hallazgo **abierto** (`Finding.isOpenFor`). `RectifyInspection` devuelve los `FindingId` levantados.
- La elegibilidad vuelve a una sola regla: cerrada, alcance completo y sin hallazgos bloqueantes abiertos. Se
  elimina `IneligibilityReason.UNADDRESSED_NON_CONFORMITIES` y `Finding.raisedFrom`.
- Cambia S6: antes rectificar no tocaba los hallazgos; ahora levanta los que introduce (los anteriores siguen sin tocarse).
- Tests: `CertificationTest.rectificationThatIntroducesABlockingNonConformityRaisesItsFindingAndRemovesEligibility`,
  `aClosedFindingDoesNotCoverANonConformityReintroducedByARectification`, `aRectificationThatFixesTheAnswerRaisesNoFinding`.

**16.2. Los comandos de los agregados seguían siendo públicos.** `findById(id).get().close(...)` compilaba
desde cualquier clase de `application`, así que la corrección 1 estaba garantizada por convención.
- `recordAnswer`, `attachEvidence`, `addObservation`, `close`, `rectify`, `planAction`, `verifyAction`,
  `suspend`, `renew`, `ensureRenewableWith` y `pullAuditEntries` pasan a visibilidad de paquete.
- Nuevos servicios de dominio `InspectionService`, `FindingService`, `CertificateService` y `AssetService`:
  ejecutan el comando, producen los hechos que arrastra y vuelcan la bitácora. Los casos de uso buscan por id,
  llaman al servicio y guardan. Nueva decisión D14 en `DESIGN.md`.

**16.3. La auditoría dependía de que cada caso de uso se acordara.** `pullAuditEntries()` era público y
destructivo, `RegisterAsset` no auditaba nada.
- El volcado pasa a los servicios de dominio (16.2), así que ya no se puede omitir.
- `AssetService` registra `AssetAudit.REGISTERED` en un `AuditLog<AssetCode>`. `RegisterAsset` recibe quién da
  de alta el activo. Test: `EndToEndFlowTest.registeringAnAssetIsAudited`.

**16.4. Una emisión o renovación fallida consumía un número de certificado.** `certificateNumbering.next()` se
evaluaba como argumento antes que `new ValidityPeriod(...)`, que puede fallar.
- La vigencia se construye y se valida antes de pedir el número, en emisión y en renovación.
- `ensureRenewableWith` exige además que la inspección nueva sea del mismo activo, antes de numerar.
- Tests: `CertificationTest.anInvalidValidityPeriodConsumesNoCertificateNumber`,
  `aRenewalWithAnInvalidValidityPeriodConsumesNoCertificateNumber`.

**16.5. Un certificado suspendido no vencía nunca.** `CertificateLifecycle.SUSPENDED.statusOn` ignoraba la
vigencia. Ahora devuelve `EXPIRED` fuera de ella (S10). Test:
`CertificationTest.aSuspendedCertificateIsExpiredOnceItsValidityIsOver`.

**16.6. El activo se referenciaba por objeto y se comparaba por `equals` de record.** `findByAsset(Asset)` y
`findCurrentFor(Asset, fecha)` comparaban nombre, ubicación, responsable y características, así que editar un
activo habría dejado sus hallazgos invisibles y al activo certificable.
- `Inspection`, `Finding` y `Certificate` guardan `AssetCode`; los puertos consultan por código;
  `Finding.concerns` y `Certificate.certifies` comparan códigos.
- `InspectionView` expone `assetCode()` y `assetResponsible()` (el responsable al asignar, que es a quien se
  atribuyen los hallazgos). Los informes que necesitan el nombre del activo lo buscan en `AssetRepository`.

**16.7. Reglas de negocio que seguían en los interactors.** "El acta solo se emite sobre una inspección
cerrada" pasa a `Inspection.ensureClosed()`; "un activo no se registra dos veces" a `AssetService`; el rechazo
por elegibilidad, duplicado en emitir y renovar, al método `default CertificationEligibility.ensureEligible`.

**16.8. Colecciones de primera clase donde hay invariante.** La devolución marcaba que varios return types podrían ser agregados con restricciones propias. Se agregan `Revisions` (numeración desde 1, la original no cambia, lanza `InspectionNotClosedException` si no hay ninguna) y `CorrectiveActions` (numeración dentro del hallazgo, búsqueda por id, cancelar las abiertas al aceptar una); `Inspection` y `Finding` delegan en ellas. `suspensions()`, `verifications()` y `evidence()` quedan como `List` inmutable, con el criterio escrito en la sección 4 de `DESIGN.md`. Test: `RevisionsTest`.

**Documentación:** `DESIGN.md` (S4, S5, S6, S10, S11, S15, D3, D7, D8, D9, D10, D13, D14 nueva, tabla de tests
y limitaciones), `README.md` y `docs/certiflow-domain.puml`. Total: 116 tests.

---

## 1. Cierre de inspección sin hallazgos y rectificación que deja certificable un rechazo

**Corrección:** `Inspection.close()` es público y la invariante "toda no conformidad genera un hallazgo" vivía
en `CloseInspection`. Cerrar directo el agregado no levantaba hallazgos y la inspección se podía certificar.
`RectifyInspection` podía dejar certificable una inspección que pasó a tener un criterio rechazado, porque la
elegibilidad solo miraba los hallazgos del repositorio.

**Cambios:**
- Nuevo evento de dominio `InspectionClosed(inspection, nonConformities, closedBy, closedAt)`. `Inspection.close()`
  lo devuelve en lugar de `void`. `AuditedInspection` lo reemite con sí misma como origen.
- `AuditedFindingGenerator.generate(InspectionClosed)` genera todos los hallazgos del cierre. `CloseInspection`
  quedó reducido a cerrar, generar y guardar.
- `Finding.raisedFrom(inspection, nonConformity)` indica si un hallazgo corresponde a una no conformidad dada.
- `NoBlockingFindingsEligibility` exige que toda no conformidad bloqueante de la evaluación vigente tenga su
  hallazgo; si no, lanza `UnaddressedNonConformitiesPreventCertificationException`.
- Tests: `CertificationTest.inspectionClosedWithoutRaisingItsFindingsIsNotEligible`,
  `rectificationThatIntroducesABlockingNonConformityRemovesEligibility`.
- `DESIGN.md`: S4, S5, S6, D11 y tabla de tests.

**Pendiente:** `close()` sigue siendo invocable fuera del caso de uso; se resuelve con el paso a ids (corrección 13).

## 2. Acciones huérfanas al aceptar una acción correctiva

**Corrección:** `StandardFinding.isOpen()` es `noneMatch(CorrectiveAction::isClosed)`: la primera acción aceptada
cierra el hallazgo, las demás ya no se pueden verificar y siguen contando como vencidas en `FindingsSummary`.

**Decisión:** al aceptar una acción se cancelan las demás acciones abiertas del hallazgo.

**Cambios:**
- Nuevo value object `Cancellation(cancelledBy, cancelledAt)`.
- `CorrectiveAction` suma `cancel(...)` (de paquete), `isCancelled()`, `isOpen()` y `cancellation()`. Una acción
  cancelada no se puede verificar ni volver a cancelar. `isOverdueOn` solo considera acciones abiertas.
- `StandardFinding.verifyAction` cancela las acciones abiertas restantes cuando la verificación es aceptada,
  con el verificador y el instante de la verificación.
- `FindingAudit.VERIFICATION_ACCEPTED` indica que las acciones restantes se cancelaron.
- Tests: `CorrectiveActionTest.acceptingAnActionCancelsTheOtherOpenActionsOfTheFindingSoNoneIsLeftOverdue`,
  `rejectedVerificationCancelsNothing`.
- `DESIGN.md`: S8 y tabla de tests.

## 3. Reglas que el modelo no protegía

**Corrección:** una verificación `REJECTED` sin notas era válida; se podía planificar una acción con `dueDate` en el
pasado; se podía suspender un certificado vencido; se podía renovar con la misma inspección o con una anterior
(7.3); y si `renew` fallaba, el número que dio `next()` ya se había consumido.

**Cambios:**
- `VerificationResult.requiresNotes()` (`REJECTED` lo exige). `Verification` valida en su constructor y lanza
  `VerificationNotesRequiredException`.
- `Finding.planAction` y `CorrectiveAction` reciben `plannedOn` (lo calcula `PlanCorrectiveAction` con el `Clock`).
  Un `dueDate` anterior lanza `DueDateInThePastException`; vencer el mismo día se permite.
- `Certificate.suspend` recibe `suspendedOn` y solo suspende si `statusOn(suspendedOn)` es `ACTIVE`; reemplaza al
  chequeo sobre `lifecycle`, que dejaba pasar los vencidos.
- Nuevo `Certificate.ensureRenewableWith(newBasis)`: rechaza un certificado ya renovado y exige que la inspección
  nueva se haya cerrado después que la del certificado actual (`RenewalRequiresLaterInspectionException`).
  `renew` lo invoca, y `RenewCertificate` lo llama antes de `certificateNumbering.next()`.
- Tests: `CorrectiveActionTest.rejectedVerificationMustExplainWhyAndIsNotRecordedOtherwise`,
  `actionCanBeDueTodayButNotBeforeItIsPlanned`; `CertificationTest.expiredCertificateCannotBeSuspended`,
  `renewalRequiresAnInspectionClosedAfterTheOneBehindTheCurrentCertificate`,
  `failedRenewalDoesNotConsumeACertificateNumber`.
- `DESIGN.md`: S8, S10, S11 y tabla de tests.

## 4. Excepciones inconsistentes

**Corrección:** `Asset`, `Person` y `CorrectiveAction` lanzaban `IllegalArgumentException`; el resto,
`DomainException`.

**Cambios:**
- Nueva `RequiredValueMissingException extends DomainException` en `models.shared`.
- Reemplaza a `IllegalArgumentException` en los 13 lugares que validaban un texto obligatorio vacío: `Asset`,
  `AssetCode`, `AssetType`, `Location`, `Person`, `CorrectiveAction`, `Suspension`, `CertificateNumber`,
  `InspectionAssignment`, `Observation`, `OptionAnswer`, `Unit` y `Evidence`. Se usa una sola clase porque es
  la misma regla en todos los casos.
- Tests: `RequiredValueTest` (persona, activo y acción correctiva sin su texto obligatorio).
- `DESIGN.md`: D11 y tabla de tests.

## 5. `Person` identificada por nombre

**Corrección:** `Person(String name)` era un record: dos inspectores homónimos eran la misma persona y la regla
"verificador ≠ responsable" terminaba comparando nombres.

**Cambios:**
- Nuevo value object `PersonId(String value)`: identificador de negocio (legajo o documento).
- `Person` pasa de record a entidad `Person(PersonId id, String name)` con `equals`/`hashCode` solo por `id`.
- El fixture asigna un legajo a cada persona.
- Tests: `PersonTest` (homónimos distintos; misma persona con otro nombre),
  `CorrectiveActionTest.namesakeOfTheResponsibleCanVerifyButTheResponsibleUnderAnotherSpellingCannot`, y
  `RequiredValueTest` valida también el id vacío.
- `DESIGN.md`: S14, S15, D3 y tabla de tests.

## 6. Primitive obsession: alcance de la inspección y características del activo

**Corrección:** `InspectionAssignment.scope` era un `String` que no restringía qué secciones se inspeccionan, y
`Asset.characteristics` era un `Map<String,String>`.

**Decisión:** el alcance elige secciones del esquema; solo una inspección de alcance completo puede certificar.

**Cambios:**
- Interfaz `InspectionScope.criteriaIn(SchemaVersion)` con dos implementaciones: `FullScope` y
  `SectionsScope(Set<String> sectionTitles)`. `SchemaVersion.section(title)` lanza `SectionNotInSchemaException`
  si la sección no existe, por lo que un alcance inválido falla al asignar.
- `StandardInspection` crea respuestas solo para los criterios del alcance. Responder, adjuntar evidencia u
  observar fuera del alcance lanza `CriterionOutOfScopeException`; cerrar solo exige lo que está dentro.
- `InspectionView.coversWholeSchema()`; la elegibilidad lanza `PartialInspectionCannotCertifyException` si la
  inspección no cubre todo el esquema (aplica a emitir y renovar).
- `Characteristic(name, value)` y `Characteristics(List<Characteristic>)` con nombres únicos
  (`DuplicateCharacteristicException`, nuevo paquete `models.asset.exceptions`) y `valueOf(name)`.
- Fixture: el esquema pasa a tener dos secciones (`Pressure and seal`, `Signage`) para probar alcances parciales.
- Tests: `InspectionScopeTest`, `CharacteristicsTest`.
- `DESIGN.md`: S17, S18 y tabla de tests. `docs/certiflow-domain.puml`: `Characteristics` e `InspectionScope`.

## 7. `InspectionSchema` mutable (y nombre de `publish`)

**Corrección:** `InspectionSchema` era una clase `final` pero con una lista de versiones mutable, y el método
`publish` sugería una arquitectura orientada a eventos. Se proponía un `SchemaVersionLog` y dejar el esquema
como record, de modo que publicar cree un esquema nuevo.

**Decisión:** solo se aplica a `InspectionSchema`. El resto de la corrección (colecciones de dominio como
`Responses` e inmutabilidad de `Inspection`) se descartó.

**Cambios:**
- Nuevo `SchemaVersionLog(List<SchemaVersion>)`: inmutable, no vacío, numeración consecutiva desde 1;
  `startingWith(...)`, `append(...)` (devuelve un log nuevo) y `current()`.
- `InspectionSchema` pasa a `record(name, assetType, versionLog)`. `publish` se renombra a
  `withNewVersion(sections, publishedAt)` y devuelve un `InspectionSchema` nuevo.
- `PublishSchemaVersion` guarda el esquema nuevo y devuelve su versión vigente.
- `InMemoryInspectionSchemaRepository` guarda por `AssetType` (un `Map`), así el esquema nuevo reemplaza al anterior.
- Tests: `SchemaVersioningTest.publishingAVersionYieldsANewSchemaAndLeavesThePreviousOneUntouched`, `SchemaVersionLogTest`.
- `DESIGN.md`: S2 y tabla de tests. `docs/certiflow-domain.puml`: `InspectionSchema` y `SchemaVersionLog`.

## 9. `Generator` → `Factory`

**Corrección:** los `...Generator` son Factories; conviene usar el vocabulario del patrón. (El renombre de
`publish` ya se hizo en la corrección 7.)

**Cambios:**
- `AuditedInspectionGenerator`, `AuditedFindingGenerator` y `AuditedCertificateGenerator` pasan a
  `AuditedInspectionFactory`, `AuditedFindingFactory` y `AuditedCertificateFactory`; su método `generate` pasa a
  `create`, y los campos de los casos de uso a `...Factory`.
- `DESIGN.md`: D7, D10, sección 4 (la fila de Factory ahora explica que se aplica Factory de creación y no
  Factory con selección) y limitaciones. `docs/certiflow-domain.puml`: métodos `create`.

## 13. Agregados sin identidad y casos de uso que reciben el agregado vivo

**Corrección:** `Inspection`, `Finding` y `Certificate` no tenían id, entre agregados se referenciaba por objeto
(`StandardFinding` guardaba `InspectionView`, `StandardCertificate` guardaba `basedOn`), los repositorios no
tenían `findById` (`InspectionRepository.findByAsset` no lo usaba nadie) y los casos de uso recibían el agregado,
por lo que quien llamaba podía saltearlos.

**Decisión:** ids como records; los genera el repositorio. `Asset` y `AuditLog` se mantienen por objeto.

**Cambios:**
- Nuevos records `InspectionId`, `FindingId` y `CorrectiveActionId` (local al hallazgo, numerado 1, 2, …).
  `Certificate` ya se identificaba por `CertificateNumber`.
- `InspectionView.id()`, `Finding.id()`, `CorrectiveAction.id()`.
- Referencias por id: `Finding` guarda `InspectionId` (`inspectionId()` reemplaza a `inspection()`,
  `raisedFrom(InspectionId, ...)`); `Certificate` guarda `InspectionReference(id, closedAt)` en `basedOn()`.
  `InspectionClosed` lleva `InspectionId` y `Asset` en lugar de la inspección, y desaparece el `from(this)` del decorador.
- Puertos: `InspectionRepository {nextId, save, findById}` (se elimina `findByAsset`),
  `FindingRepository` suma `nextId` y `findById`, `CertificateRepository` suma `findByNumber`. Implementaciones en
  memoria indexadas por id con `UUID` para `nextId`.
- Casos de uso reciben y devuelven ids: `AssignInspection(AssetCode, …) → InspectionId`, `RecordAnswer`,
  `AttachEvidence`, `AddObservation`, `RectifyInspection` y `CloseInspection(InspectionId, …) → List<FindingId>`,
  `PlanCorrectiveAction(FindingId, …) → CorrectiveActionId`, `VerifyCorrectiveAction(FindingId, CorrectiveActionId, …)`,
  `IssueCertificate(InspectionId, …) → CertificateNumber`, `RenewCertificate(CertificateNumber, InspectionId, …)`,
  `SuspendCertificate(CertificateNumber, …)`, `GenerateInspectionAct(InspectionId)`,
  `GenerateFindingsSummary(AssetCode)`, `GenerateCertificateDocument(CertificateNumber)`.
- Nuevas excepciones `AssetNotFoundException`, `InspectionNotFoundException`, `FindingNotFoundException`,
  `CertificateNotFoundException`.
- `AuditedFindingFactory.create(InspectionClosed, Supplier<FindingId>)`; `AuditedInspectionFactory.create` recibe el id.
- Tests: todos pasan a trabajar con ids (el fixture suma `inspection(id)`, `finding(id)`, `action(...)`,
  `certificate(number)`); nuevo `AggregateIdentityTest`.
- `DESIGN.md`: S15, D3 (reescrita), D9, D13 y tabla de tests. `docs/certiflow-domain.puml`: repositorios, ids y
  referencias.
- Queda resuelto el pendiente de la corrección 1: `close()` ya no se invoca desde afuera porque los casos de uso
  no reciben el agregado.

## 8. "Cero if" como dogma

**Corrección:** la tabla `Map<VerificationResult, FindingAudit>` en `AuditedFinding` era un `switch` disfrazado;
era más simple que `VerificationResult` conociera su `FindingAudit`. Además `StandardCertificate.suspend` usaba un
`if` sobre el estado, lo que contradecía lo que decía `DESIGN.md`.

**Cambios:**
- `VerificationResult` suma `audit()`: `ACCEPTED → VERIFICATION_ACCEPTED`, `REJECTED → VERIFICATION_REJECTED`.
  Se elimina el `Map` de `AuditedFinding`, que ahora registra `verificationResult.audit()`.
- `DESIGN.md` D8 reescrita: polimorfismo cuando la variación es por tipo y crece; `if` común para guardas y
  decisiones puntuales (la guarda de `suspend`, que desde la corrección 3 mira `statusOn`, y la cancelación de
  acciones de la corrección 2). Se deja de presentar "cero `if`" como regla.
- Los tests existentes de auditoría de verificaciones (`CorrectiveActionTest`) cubren el cambio.

## 10 y 11. Elegibilidad como servicio de dominio con decisión explicable

**Corrección 10:** `CertificationEligibility.ensureEligible` tiraba una excepción en vez de devolver una decisión
explicable (`CertificationDecision`).

**Corrección 11:** había reglas de negocio fuera del dominio: `CertificationEligibility`,
`NoBlockingFindingsEligibility` y excepciones como `AssetAlreadyCertifiedException` estaban en `application`.
"No certificar con hallazgos bloqueantes" es una regla del negocio, por lo tanto un servicio de dominio.

**Cambios:**
- Nuevo paquete `models.certification` (servicio de dominio): `CertificationEligibility.evaluate(InspectionView,
  List<Finding>) → CertificationDecision` y su implementación `NoBlockingFindingsEligibility`. Ya no depende del
  repositorio: el caso de uso le pasa los hallazgos del activo.
- `CertificationDecision(List<Ineligibility>)` con `isEligible()` e `isBlockedBy(reason)`;
  `Ineligibility(reason, detail)`; enum `IneligibilityReason` (`INSPECTION_NOT_CLOSED`, `PARTIAL_INSPECTION`,
  `UNADDRESSED_NON_CONFORMITIES`, `BLOCKING_FINDINGS`). La decisión informa todos los motivos a la vez.
- `NotEligibleForCertificationException` (lleva la decisión) reemplaza a `BlockingFindingsPreventCertificationException`,
  `UnaddressedNonConformitiesPreventCertificationException` y `PartialInspectionCannotCertifyException`, que se eliminan.
- Nuevo caso de uso `EvaluateCertification(InspectionId) → CertificationDecision` para consultar sin emitir.
  `IssueCertificate` y `RenewCertificate` reciben `FindingRepository`, piden la decisión y fallan con ella.
- Se vacía `application.exceptions`: `AssetAlreadyCertifiedException` y `CertificateNotFoundException` →
  `models.certificate.exceptions`; `AssetAlreadyRegisteredException` y `AssetNotFoundException` →
  `models.asset.exceptions`; `NoInspectionSchemaForAssetTypeException` y `SchemaAlreadyDefinedForAssetTypeException` →
  `models.schema.exceptions`; `InspectionNotFoundException` → `models.inspection.exceptions`;
  `FindingNotFoundException` → `models.finding.exceptions`. Se elimina `application.certification`.
- Tests: los que esperaban las excepciones eliminadas verifican el motivo con `CertiflowFixture.assertNotEligible`;
  nuevo `CertificationDecisionTest`.
- `DESIGN.md`: S5, S17, sección 2, D10, D11 y tabla de tests. `docs/certiflow-domain.puml`: paquete
  `models.certification`.

## 12. Observaciones de casos de uso: auditoría de esquemas y reglas de asignación

**Corrección:** no existía un audit log para la creación de un esquema; el comportamiento de esquemas podía
estar tras un servicio de dominio en lugar de que los casos de uso llamen al modelo; `AssignInspection` admitía
que creador e inspector fueran la misma persona, que el responsable del activo asignara la inspección y que el
responsable inspeccionara su propio activo. En `RegisterAsset`, la unicidad por consulta no escala con varios hilos.

**Cambios:**
- Nuevo servicio de dominio `InspectionSchemaService` (`models.schema`) con `create(...)` y `addVersion(...)`,
  que audita en un `AuditLog<AssetType>` (`SchemaAudit.CREATED`, `SchemaAudit.VERSION_ADDED`, con autor y fecha).
  `CreateInspectionSchema` y `PublishSchemaVersion` lo usan y reciben quién realiza la operación.
- `InMemoryAuditLog` indexa con `HashMap` (por `equals`) en lugar de `IdentityHashMap`, para poder usar la
  identidad de negocio del esquema como clave. Los decoradores no redefinen `equals`, así que su historial no cambia.
- `StandardInspection` recibe quién asigna y rechaza los tres escenarios: `AssignerCannotInspectException`,
  `AssetResponsibleCannotAssignInspectionException`, `AssetResponsibleCannotInspectOwnAssetException`.
- `DESIGN.md`: se documenta como limitación que la unicidad de `RegisterAsset`/`CreateInspectionSchema` requiere
  una restricción en la persistencia con concurrencia.
- Tests: `InspectionAssignmentTest`, `SchemaVersioningTest.creatingTheSchemaAndAddingVersionsIsAudited`.
- `DESIGN.md`: S2, S19, D7, limitaciones y tabla de tests.
- **No aplicado:** mover el comportamiento de `Inspection` (RecordAnswer) a servicios de dominio y hacerla
  inmutable con `Responses`, por decisión del equipo (ver corrección 7).

## 15. Decoradores `Audited*` que reenvían todo a mano

**Corrección (SRP/OCP):** cada `Audited*` reenviaba a mano todas las consultas. Agregar un método a `Inspection`
obligaba a tocar interfaz + `Standard*` + `Audited*`.

**Cambios:**
- Se eliminan las interfaces `Inspection`, `Finding` y `Certificate` y los decoradores `AuditedInspection`,
  `AuditedFinding` y `AuditedCertificate`. `StandardInspection`, `StandardFinding` y `StandardCertificate` pasan
  a ser las clases públicas `Inspection` (implementa `InspectionView`), `Finding` y `Certificate`, con constructor
  de paquete.
- Nuevo `AuditTrail` (`models.audit`): cada agregado anota una `AuditEntry` al terminar cada comando (y al nacer)
  y la entrega con `pullAuditEntries()`. Los textos y el orden de la bitácora no cambian.
- `AuditLog` suma `recordAll(subject, entries)`. Los casos de uso que modifican agregados reciben su `AuditLog`
  y vuelcan las entradas después de guardar.
- Las fábricas pasan a `InspectionFactory`, `FindingFactory` y `CertificateFactory`: solo crean, ya no reciben
  el `AuditLog`.
- Agregar un método a un agregado ahora toca una sola clase.
- Tests: `AuditTrailTest`; los tests de historial existentes siguen pasando sin cambios.
- `DESIGN.md`: sección 2, D6, D7 (reescrita), D8, D13, sección 4, limitaciones y tabla de tests.
  `docs/certiflow-domain.puml` y `README.md` sin decoradores.

## 14. Arquitectura: módulos, interfaces de entrada y dirección de dependencias

**Corrección:** el módulo de usecases dependía del de aplicación, cuando debería ser al revés (application →
usecases, con la aplicación implementando las interfaces). El dominio debe comprender toda la lógica de negocio
(models, services, exceptions) e incluir las interfaces de ports y usecases; las implementaciones concretas van
fuera. Los casos de uso eran clases concretas sin interfaz de entrada, y todo estaba en un único módulo Maven,
así que nada impedía que un paquete importara a otro.

**Cambios:**
- Build Maven multi-módulo: `pom.xml` raíz (`packaging pom`) con `certiflow-domain`, `certiflow-application` y
  `certiflow-details`.
- `certiflow-domain`: `models` (con servicios de dominio y excepciones), `ports` (salida) y `usecases`
  (entrada). Cada caso de uso pasa a ser una **interfaz** con su `execute`. Los modelos de lectura
  (`InspectionAct`, `ActLine`, `FindingsSummary`, `FindingLine`, `CertificateDocument`) pasan de
  `application.report` a `usecases.report`, porque son el tipo de retorno de esas interfaces.
- `certiflow-application`: `application.<concepto>.XxxInteractor implements Xxx`, con la lógica que antes tenía
  cada clase de caso de uso. Depende solo de `certiflow-domain` (y de `certiflow-details` en scope test).
- `certiflow-details`: `details.inmemory`, depende solo de `certiflow-domain`.
- Verificado: el dominio no importa `application` ni `details`, y un import prohibido no compila
  (`package ar.edu.itba.certiflow.details.inmemory does not exist`).
- Tests: unitarios del modelo en `certiflow-domain`; integración y `CertiflowFixture` (raíz de composición, que
  instancia los `...Interactor` y expone las interfaces) en `certiflow-application`. `RequiredValueTest` se movió
  a `application` porque usa el fixture.
- `.gitignore`: ignora `target/` de todos los módulos.
- `DESIGN.md`: sección 2 reescrita. `README.md`: estructura por módulos. `docs/certiflow-domain.puml`: interfaces
  de casos de uso e interactors.

---

# Entrega 2 · Fase 1: DDD, persistencia, API REST y CI

Cambios hechos para la Entrega 2, siguiendo
[docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md](docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md) y su
[plan](docs/plans/2026-10-12-fase-1-ddd-persistencia-api.md). Ninguno cambia reglas de negocio.

## E2.1. Paquetes por bounded context y capas DDD
- `models.*`, `ports` y `usecases` pasan a `<contexto>.model`, `<contexto>.repository` y `<contexto>.usecase`
  (`catalog`, `schema`, `inspection`, `certification`, `shared`). `models.evaluation` se une a `schema.model`;
  `models.finding` a `inspection.model`; `models.certificate` y `models.certification` a `certification.model`.
- Las interfaces de caso de uso se llaman `XxxUseCase`; sus implementaciones, `XxxUseCaseImpl`. El módulo
  `certiflow-details` pasa a `certiflow-infrastructure`.
- DESIGN.md: sección 2 reescrita, D15, glosario.

## E2.2. Respuesta cruda y criterio por código
- `RawAnswer` (sealed), `ApprovalRule.answerFrom`, `Criterion.answerFrom`, `Answer.raw()`,
  `SchemaVersion.criterion(code)`, `AnswerTypeMismatchException`; sobrecargas por código en `InspectionService`.
- Tests: `RawAnswerTest`. DESIGN.md: D17.

## E2.3. Casos de uso con comandos
- Un `XxxCommand` por caso de uso de escritura; el fixture gana helpers (`answer`, `close`, `issue`…).
- Tests: `InspectionExecutionTest.answeringAnUnknownCriterionCodeIsRejectedAndChangesNothing`,
  `answeringWithTheWrongTypeOfValueIsRejectedAndChangesNothing`. DESIGN.md: D17.

## E2.4. Auditoría indexada por id
- `AuditLog<InspectionId>`, `AuditLog<FindingId>`, `AuditLog<CertificateNumber>`. Elimina la limitación
  "AuditLog indexado por objeto".
- Tests: `AggregateIdentityTest.historyIsFoundByIdentityEvenFromAnotherInstanceOfTheSameAggregate`. DESIGN.md: D3, D7.

## E2.5. Capa anticorrupción entre Inspección y Certificación
- `InspectionResult`, `OpenNonConformity`, `InspectionResultTranslator` y su implementación en `application`.
  Certificación deja de importar `Finding` e `InspectionView`. Emitir ya no lanza `InspectionOfAnotherAssetException`
  (el activo sale del resultado); renovar sí.
- Tests: `NoBlockingFindingsEligibilityTest`, `InspectionResultTranslatorImplTest` (Mockito). DESIGN.md: D16.

## E2.6. Snapshots
- `InspectionSnapshot`, `FindingSnapshot`, `CorrectiveActionSnapshot`, `CertificateSnapshot`; `snapshot()` y
  `XxxFactory.restore`. `CertificateLifecycle` pasa a público.
- Tests: `SnapshotTest`. DESIGN.md: D18.

## E2.7. Casos de uso de lectura
- `FindAsset`, `ListAssets`, `FindSchema`, `FindInspection`, `FindFinding`, `ListFindings`, `ListCertificates`,
  `FindCertificate`, `AuditHistory`; los repositorios ganan `findAll` y `CertificateRepository.findByAsset`.
- Tests: `QueriesTest`.

## E2.8–E2.10. Persistencia con JPA y Flyway
- Spring Boot 4.1.1 (BOM), Spring Data JPA, Flyway, PostgreSQL y H2. Esquema en `V1__init.sql`.
- Entidades y adaptadores por contexto; secciones de una versión y evaluación de una revisión en JSON;
  numeración de certificados por secuencia; `JpaAuditLog`.
- Tests: `JpaAssetRepositoryTest`, `JpaInspectionSchemaRepositoryTest`, `SectionsJsonTest`,
  `JpaInspectionRepositoryTest`, `JpaFindingRepositoryTest`, `JpaCertificateRepositoryTest`, `JpaAuditLogTest`.
  DESIGN.md: D19.

## E2.11–E2.14. Módulo web y API REST
- `certiflow-web`: composición (`UseCaseConfiguration`), proxy transaccional (`TransactionalUseCases`), persona
  por headers, errores RFC 9457, controladores de activos, esquemas, inspecciones, hallazgos y certificados con
  HAL, links según el estado, `ETag` y paginación. Se compila con `-parameters`.
- Elimina la limitación "auditar y guardar no son atómicos".
- Tests: `TransactionalUseCasesTest`, `RootControllerTest`, `AssetApiTest`, `SchemaApiTest`, `InspectionApiTest`,
  `FindingApiTest`, `CertificateApiTest`, `EndToEndApiTest`. DESIGN.md: D20, D21.

## E2.15. Tests parametrizados
- Vencimiento de acciones y de certificados, y severidad → resultado, como `@ParameterizedTest`.

## E2.16. CI y base local
- `.github/workflows/ci.yml` (`mvn -B clean verify`) y `docker-compose.yml` con PostgreSQL 17.

## E2.17. Correcciones de la revisión final
- Paginación con `page < 0` o `size < 1` responde 400 en lugar de 500 (`PageLinks`).
- Una regla incompleta en un esquema (sin unidad, sin límites, `expected` inválido, sin opciones) o un elemento
  `null` en una lista del request responde 400 en lugar de 500 (`SchemaPayloads`, `RequestFields.requireElements`).
- Un valor más largo que su columna responde 400 y una clave duplicada 409, en `problem+json` (`ApiExceptionHandler`).
- El resumen de hallazgos, el documento del certificado y las razones de no elegibilidad se devuelven con recursos
  propios (`FindingsSummaryResource`, `CertificateDocumentResource`), sin serializar objetos de dominio.
- La escala de los decimales se pierde al leer el JSON: la propiedad `use-big-decimal-for-floats` no tiene efecto en
  Spring Boot 4. Se activa con un `JsonMapperBuilderCustomizer`. Los tests de API usan ahora la configuración real
  del módulo web.
- Tests: `AssetApiTest.invalidPagingIs400`, `aValueTooLongForStorageIs400`, `SchemaApiTest.anIncompleteRuleIs400`,
  `aNullSectionIs400`, `InspectionApiTest.aFailureAfterTheAuditEntryIsWrittenRollsTheEntryBack`,
  `theDecimalScaleReachesTheClient`, `FindingApiTest.theFindingsSummaryUsesPlainValues`,
  `CertificateApiTest.theDocumentUsesPlainValues`.

## E2.18. Listado de inspecciones
- El punto de entrada anunciaba `/api/v1/inspections` pero un `GET` respondía 405. Nuevo
  `GET /api/v1/inspections?asset=&status=open|closed&page=&size=` (`ListInspectionsUseCase`,
  `InspectionRepository.findByAsset` y `findAll`). Un `status` desconocido responde 400.
- Test: `InspectionApiTest.inspectionsAreListedByAssetAndStatusInPages`.

## E2.19. 404 coherentes
- Una revisión o una versión de esquema inexistente responde 404 en `problem+json` (`ResourceNotFoundException`),
  como el resto de los recursos.
- Un criterio que existe en el esquema pero queda fuera del alcance de la inspección responde 404 tanto al leerlo
  como al responderlo (`CriterionOutOfScopeException` → 404); antes el `PUT` daba 409.
- Tests: `InspectionApiTest.aMissingRevisionIs404AsProblemJson`, `answeringACriterionOutsideTheScopeIs404LikeReadingIt`,
  `SchemaApiTest.aMissingVersionIs404AsProblemJson`.
