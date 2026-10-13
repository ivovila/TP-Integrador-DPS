import type { AuditEntry, AuditSubject } from '../../domain/audit';
import type { Instant, Person } from '../../domain/shared';

const auditLabels = {
  REGISTERED: 'Asset registered',
  CREATED: 'Inspection schema created',
  VERSION_ADDED: 'Schema version added',
  ASSIGNED: 'Inspection assigned',
  ANSWER_RECORDED: 'Answer recorded',
  EVIDENCE_ATTACHED: 'Evidence attached',
  OBSERVATION_ADDED: 'Observation added',
  CLOSED: 'Inspection closed',
  RECTIFIED: 'Inspection rectified',
  RAISED: 'Finding raised',
  ACTION_PLANNED: 'Corrective action planned',
  VERIFICATION_ACCEPTED: 'Verification accepted: action and finding closed, remaining actions cancelled',
  VERIFICATION_REJECTED: 'Verification rejected: action remains open',
  ISSUED: 'Certificate issued',
  SUSPENDED: 'Certificate suspended',
  RENEWED: 'Certificate renewed',
} as const;

export type AuditActionName = keyof typeof auditLabels;

function keyOf(subject: AuditSubject): string {
  return Object.values(subject).join(':');
}

export class InMemoryAuditLog {
  private readonly entriesBySubject = new Map<string, AuditEntry[]>();

  record(subject: AuditSubject, action: AuditActionName, performedBy: Person, occurredAt: Instant, detail: string): void {
    const entry: AuditEntry = { action, label: auditLabels[action], performedBy: { ...performedBy }, occurredAt, detail };
    this.entriesBySubject.set(keyOf(subject), [...this.entriesFor(subject), entry]);
  }

  entriesFor(subject: AuditSubject): AuditEntry[] {
    return this.entriesBySubject.get(keyOf(subject)) ?? [];
  }
}
