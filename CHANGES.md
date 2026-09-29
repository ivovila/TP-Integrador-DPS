# Cambios a partir de las correcciones (Entrega 1)

Registro de lo que se modificó en respuesta a `Corrections.md`, en el orden en que se resolvió.

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
