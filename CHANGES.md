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
