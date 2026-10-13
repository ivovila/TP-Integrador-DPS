import type { AuditEntry } from './audit';
import type { Criterion, EvidenceKind, Outcome, RawAnswer } from './schema';
import type { Instant, LocalDate, Person } from './shared';

export type InspectionScope = { kind: 'full' } | { kind: 'sections'; titles: string[] };

export interface InspectionAssignment {
  inspector: Person;
  plannedDate: LocalDate;
  scope: InspectionScope;
}

export interface Evidence {
  kind: EvidenceKind;
  reference: string;
  description: string;
}

export interface AttachedEvidence {
  evidence: Evidence;
  attachedBy: Person;
  attachedAt: Instant;
}

export interface Observation {
  text: string;
  observedBy: Person;
  observedAt: Instant;
}

export interface GivenAnswer {
  value: RawAnswer;
  answeredBy: Person;
  answeredAt: Instant;
}

export interface CriterionResponse {
  sectionTitle: string;
  criterion: Criterion;
  given?: GivenAnswer;
  attachments: AttachedEvidence[];
  observations: Observation[];
  outcome: Outcome;
}

export interface CriterionOutcome {
  criterionCode: string;
  outcome: Outcome;
}

export interface Revision {
  number: number;
  reason: string;
  sealedBy: Person;
  sealedAt: Instant;
  outcomes: CriterionOutcome[];
}

export type InspectionAction =
  | 'recordAnswer'
  | 'attachEvidence'
  | 'addObservation'
  | 'closeInspection'
  | 'rectifyInspection'
  | 'generateInspectionAct';

export interface InspectionSummary {
  id: string;
  assetCode: string;
  assignment: InspectionAssignment;
  schemaVersion: number;
  closed: boolean;
  coversWholeSchema: boolean;
}

export interface InspectionDetails extends InspectionSummary {
  responses: CriterionResponse[];
  revisions: Revision[];
  actions: InspectionAction[];
}

export interface ActLine {
  criterionCode: string;
  criterionText: string;
  criterion: Criterion;
  answer: RawAnswer;
  answeredBy: Person;
  answeredAt: Instant;
  evidenceCount: number;
  observations: string[];
  outcome: Outcome;
}

export interface InspectionAct {
  inspectionId: string;
  assetCode: string;
  assetName: string;
  assignment: InspectionAssignment;
  schemaVersion: number;
  lines: ActLine[];
  revisions: Revision[];
  history: AuditEntry[];
}
