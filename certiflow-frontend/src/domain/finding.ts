import type { AttachedEvidence } from './inspection';
import type { Criterion, Severity } from './schema';
import type { Instant, LocalDate, Person } from './shared';

export type VerificationResult = 'ACCEPTED' | 'REJECTED';

export interface Verification {
  verifier: Person;
  result: VerificationResult;
  notes: string;
  verifiedAt: Instant;
}

export type CorrectiveActionState = 'OPEN' | 'CLOSED' | 'CANCELLED';
export type CorrectiveActionAction = 'verifyCorrectiveAction';

export interface CorrectiveAction {
  id: number;
  description: string;
  responsible: Person;
  dueDate: LocalDate;
  plannedBy: Person;
  plannedAt: Instant;
  state: CorrectiveActionState;
  overdue: boolean;
  verifications: Verification[];
  actions: CorrectiveActionAction[];
}

export type FindingAction = 'planCorrectiveAction';

export interface Finding {
  id: string;
  inspectionId: string;
  assetCode: string;
  criterion: Criterion;
  severity: Severity;
  responsible: Person;
  open: boolean;
  blocksCertification: boolean;
  evidence: AttachedEvidence[];
  correctiveActions: CorrectiveAction[];
  actions: FindingAction[];
}

export interface FindingLine {
  findingId: string;
  criterionCode: string;
  description: string;
  severity: string;
  responsible: Person;
  open: boolean;
  blocksCertification: boolean;
  overdueActions: number;
}

export interface FindingsSummary {
  assetCode: string;
  findings: FindingLine[];
  openFindings: number;
  blockingFindings: number;
}
