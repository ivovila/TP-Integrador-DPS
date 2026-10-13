import type { CorrectiveAction, CorrectiveActionState, Finding, FindingLine, Verification } from '../../domain/finding';
import type { Instant, Person } from '../../domain/shared';
import type { FindingPort } from '../../ports/FindingPort';
import { storedAssetWithCode } from './catalog';
import { subsystemOfResponse } from './inspection';
import { outcomeOf } from './rules';
import type { InMemorySystem, StoredCorrectiveAction, StoredFinding, StoredInspection, StoredResponse } from './store';
import { isSamePerson, reject, requireText } from './support';

function isAccepted(verification: Verification): boolean {
  return verification.result === 'ACCEPTED';
}

function stateOf(action: StoredCorrectiveAction): CorrectiveActionState {
  if (action.verifications.some(isAccepted)) {
    return 'CLOSED';
  }
  return action.cancellation === undefined ? 'OPEN' : 'CANCELLED';
}

function isOpenAction(action: StoredCorrectiveAction): boolean {
  return stateOf(action) === 'OPEN';
}

export function isOpenFinding(finding: StoredFinding): boolean {
  return !finding.correctiveActions.some((action) => stateOf(action) === 'CLOSED');
}

export function findingsOfAsset(system: InMemorySystem, assetCode: string): StoredFinding[] {
  return [...system.findings.values()].filter((finding) => finding.assetCode === assetCode);
}

export function raiseFinding(
  system: InMemorySystem,
  inspection: StoredInspection,
  nonConformity: StoredResponse,
  raisedBy: Person,
  raisedAt: Instant,
): string {
  const finding: StoredFinding = {
    id: `FND-${system.findings.size + 1}`,
    inspectionId: inspection.id,
    assetCode: inspection.assetCode,
    criterion: structuredClone(nonConformity.criterion),
    subsystemCode: subsystemOfResponse(inspection, nonConformity),
    evidence: structuredClone(nonConformity.attachments),
    responsible: { ...inspection.assetResponsible },
    correctiveActions: [],
  };
  system.findings.set(finding.id, finding);
  system.auditLog.record(
    { kind: 'finding', id: finding.id },
    'RAISED',
    raisedBy,
    raisedAt,
    `${finding.criterion.code} ${outcomeOf(nonConformity)}`,
  );
  return finding.id;
}

function storedFindingWithId(system: InMemorySystem, findingId: string): StoredFinding {
  return system.findings.get(findingId) ?? reject('FindingNotFoundException', `Finding ${findingId} does not exist`);
}

function correctiveActionView(system: InMemorySystem, action: StoredCorrectiveAction): CorrectiveAction {
  const { cancellation, ...actionData } = action;
  const open = isOpenAction(action);
  return {
    ...actionData,
    state: stateOf(action),
    overdue: open && system.clock.today() > action.dueDate,
    actions: open ? ['verifyCorrectiveAction'] : [],
  };
}

function findingView(system: InMemorySystem, finding: StoredFinding): Finding {
  const open = isOpenFinding(finding);
  return structuredClone({
    ...finding,
    severity: finding.criterion.severity,
    open,
    correctiveActions: finding.correctiveActions.map((action) => correctiveActionView(system, action)),
    actions: open ? ['planCorrectiveAction' as const] : [],
  });
}

function ensureOpenFinding(finding: StoredFinding): void {
  if (!isOpenFinding(finding)) {
    reject('FindingAlreadyClosedException', 'The finding is already closed');
  }
}

export function createFindingPort(system: InMemorySystem): FindingPort {
  return {
    async listFindings(filter) {
      return [...system.findings.values()]
        .filter((finding) => filter.assetCode === undefined || finding.assetCode === filter.assetCode)
        .map((finding) => findingView(system, finding));
    },

    async findFinding(findingId) {
      return findingView(system, storedFindingWithId(system, findingId));
    },

    async planCorrectiveAction(command) {
      const finding = storedFindingWithId(system, command.findingId);
      ensureOpenFinding(finding);
      requireText(command.description, 'A corrective action must describe what will be done');
      requireText(command.responsible.id, 'A corrective action needs a responsible person');
      requireText(command.dueDate, 'A corrective action needs a due date');
      const plannedOn = system.clock.today();
      if (command.dueDate < plannedOn) {
        reject('DueDateInThePastException', `The due date ${command.dueDate} is before ${plannedOn}`);
      }
      const plannedAt = system.clock.now();
      const action: StoredCorrectiveAction = {
        id: finding.correctiveActions.length + 1,
        description: command.description,
        responsible: { ...command.responsible },
        dueDate: command.dueDate,
        plannedBy: { ...command.plannedBy },
        plannedAt,
        verifications: [],
      };
      finding.correctiveActions.push(action);
      system.auditLog.record(
        { kind: 'finding', id: finding.id },
        'ACTION_PLANNED',
        command.plannedBy,
        plannedAt,
        `${action.description}, due ${action.dueDate}`,
      );
      return action.id;
    },

    async verifyCorrectiveAction(command) {
      const finding = storedFindingWithId(system, command.findingId);
      ensureOpenFinding(finding);
      const action =
        finding.correctiveActions.find((candidate) => candidate.id === command.actionId) ??
        reject('CorrectiveActionNotInFindingException', `The finding has no corrective action ${command.actionId}`);
      if (!isOpenAction(action)) {
        reject('CorrectiveActionAlreadyClosedException', 'The corrective action is already closed');
      }
      if (isSamePerson(command.verifier, action.responsible)) {
        reject(
          'VerifierMustDifferFromResponsibleException',
          `${command.verifier.name} is responsible for the action and cannot verify it`,
        );
      }
      if (command.result === 'REJECTED' && command.notes.trim() === '') {
        reject('VerificationNotesRequiredException', 'A rejected verification must explain why');
      }
      const verifiedAt = system.clock.now();
      const verification: Verification = {
        verifier: { ...command.verifier },
        result: command.result,
        notes: command.notes,
        verifiedAt,
      };
      action.verifications.push(verification);
      if (isAccepted(verification)) {
        finding.correctiveActions.filter(isOpenAction).forEach((openAction) => {
          openAction.cancellation = { cancelledBy: { ...command.verifier }, cancelledAt: verifiedAt };
        });
      }
      system.auditLog.record(
        { kind: 'finding', id: finding.id },
        isAccepted(verification) ? 'VERIFICATION_ACCEPTED' : 'VERIFICATION_REJECTED',
        command.verifier,
        verifiedAt,
        command.notes,
      );
      return structuredClone(verification);
    },

    async generateFindingsSummary(assetCode) {
      storedAssetWithCode(system, assetCode);
      const today = system.clock.today();
      const findings: FindingLine[] = findingsOfAsset(system, assetCode).map((finding) => ({
        findingId: finding.id,
        criterionCode: finding.criterion.code,
        description: finding.criterion.text,
        severity: finding.criterion.severity.label,
        responsible: { ...finding.responsible },
        open: isOpenFinding(finding),
        subsystemCode: finding.subsystemCode,
        overdueActions: finding.correctiveActions.filter((action) => isOpenAction(action) && today > action.dueDate).length,
      }));
      return {
        assetCode,
        findings,
        openFindings: findings.filter((line) => line.open).length,
      };
    },
  };
}
