import type { CorrectiveAction, CorrectiveActionState, Finding, FindingsSummary } from '../../domain/finding';
import type { Vocabulary } from '../../domain/schema';
import { actionsOfferedBy, correctiveActionActionByRel, findingActionByRel } from './actions';
import { attachedEvidenceOf } from './inspectionMapping';
import { criterionOf, severityNamed } from './schemaMapping';
import type { WireCorrectiveAction, WireFinding, WireFindingsSummary } from './wire';

function stateOf(wire: WireCorrectiveAction): CorrectiveActionState {
  if (wire.closed) {
    return 'CLOSED';
  }
  return wire.cancelled ? 'CANCELLED' : 'OPEN';
}

export function correctiveActionOf(wire: WireCorrectiveAction): CorrectiveAction {
  return {
    id: wire.id,
    description: wire.description,
    responsible: wire.responsible,
    dueDate: wire.dueDate,
    plannedBy: wire.plannedBy,
    plannedAt: wire.plannedAt,
    state: stateOf(wire),
    overdue: wire.overdue,
    verifications: wire.verifications,
    actions: actionsOfferedBy(wire, correctiveActionActionByRel),
  };
}

export function findingOf(wire: WireFinding, vocabulary: Vocabulary): Finding {
  return {
    id: wire.id,
    inspectionId: wire.inspectionId,
    assetCode: wire.assetCode,
    criterion: criterionOf(wire.criterion, vocabulary),
    severity: severityNamed(vocabulary, wire.severity),
    responsible: wire.responsible,
    open: wire.open,
    ...(wire.subsystemCode === null ? {} : { subsystemCode: wire.subsystemCode }),
    evidence: wire.evidence.map((attached) => attachedEvidenceOf(attached, vocabulary)),
    correctiveActions: wire.actions.map(correctiveActionOf),
    actions: actionsOfferedBy(wire, findingActionByRel),
  };
}

export function findingsSummaryOf(wire: WireFindingsSummary): FindingsSummary {
  return {
    assetCode: wire.assetCode,
    findings: wire.findings.map((line) => ({
      findingId: line.findingId,
      criterionCode: line.criterionCode,
      description: line.description,
      severity: line.severity,
      responsible: line.responsible,
      open: line.open,
      ...(line.subsystemCode === null ? {} : { subsystemCode: line.subsystemCode }),
      overdueActions: line.overdueActions,
    })),
    openFindings: wire.openFindings,
  };
}
