import type { CertificateStatus, CertificationOutcome, IneligibilityReason } from '../../domain/certification';
import type { VerificationResult } from '../../domain/finding';
import type { Outcome, RuleType, YesNoAnswer } from '../../domain/schema';
import type { Instant, LocalDate } from '../../domain/shared';

export interface WireLink {
  href: string;
  templated?: boolean;
}

export interface WireResource {
  _links?: Record<string, WireLink | WireLink[]>;
}

export interface WireCollection extends WireResource {
  _embedded?: Record<string, unknown[]>;
}

export interface WirePerson {
  id: string;
  name: string;
}

export interface WireAsset extends WireResource {
  code: string;
  name: string;
  type: string;
  location: string;
  jurisdictionCode: string;
  responsible: WirePerson;
  characteristics: { name: string; value: string }[];
  subsystems: { code: string; name: string }[];
}

export interface WireRule {
  type: RuleType;
  unit?: string | null;
  minimum?: number | null;
  maximum?: number | null;
  expected?: YesNoAnswer | null;
  options?: string[] | null;
}

export interface WireEvidenceRequirement {
  kind: string;
  minimum: number;
}

export interface WireCriterion {
  code: string;
  text: string;
  severity: string;
  rule: WireRule;
  requiredEvidence: WireEvidenceRequirement[];
  subsystemCode?: string | null;
}

export interface WireSection {
  title: string;
  criteria: WireCriterion[];
  subsystemCode?: string | null;
}

export interface WireSchemaVersion extends WireResource {
  number: number;
  publishedAt: Instant;
  effectiveFrom: Instant;
  requiredSubsystems: string[];
  sections: WireSection[];
}

export interface WireSchema extends WireResource {
  name: string;
  assetType: string;
  currentVersion: number;
}

export interface WireVocabulary {
  severities: { name: string; label: string; outcomeWhenUnmet: Outcome }[];
  evidenceKinds: { name: string; label: string }[];
  jurisdictions: { code: string; policyCode: string }[];
}

export interface WireInspection extends WireResource {
  id: string;
  assetCode: string;
  inspector: WirePerson;
  plannedDate: LocalDate;
  schemaVersion: number;
  sections: string[] | null;
  closed: boolean;
  coversWholeSchema: boolean;
}

export type WireAnswer = number | boolean | string;

export interface WireAttachedEvidence {
  kind: string;
  reference: string;
  description: string;
  attachedBy: WirePerson;
  attachedAt: Instant;
}

export interface WireObservation {
  text: string;
  observedBy: WirePerson;
  observedAt: Instant;
}

export interface WireCriterionResponse extends WireResource {
  criterion: WireCriterion;
  answer: WireAnswer | null;
  answeredBy: WirePerson | null;
  answeredAt: Instant | null;
  evidence: WireAttachedEvidence[];
  observations: WireObservation[];
  outcome: Outcome;
}

export interface WireRevision extends WireResource {
  number: number;
  reason: string;
  sealedBy: WirePerson;
  sealedAt: Instant;
  outcomes: Record<string, Outcome>;
}

export interface WireAuditEntry {
  action: string;
  label: string;
  performedById: string;
  performedByName: string;
  occurredAt: Instant;
  detail: string;
}

export interface WireActLine {
  criterionCode: string;
  criterionText: string;
  answer: WireAnswer;
  answeredBy: WirePerson;
  answeredAt: Instant;
  evidenceCount: number;
  observations: string[];
  outcome: Outcome;
}

export interface WireInspectionAct extends WireResource {
  assetCode: string;
  assetName: string;
  schemaVersion: number;
  lines: WireActLine[];
  history: WireAuditEntry[];
}

export interface WireVerification {
  verifier: WirePerson;
  result: VerificationResult;
  notes: string;
  verifiedAt: Instant;
}

export interface WireCorrectiveAction extends WireResource {
  id: number;
  description: string;
  responsible: WirePerson;
  dueDate: LocalDate;
  plannedBy: WirePerson;
  plannedAt: Instant;
  open: boolean;
  closed: boolean;
  cancelled: boolean;
  overdue: boolean;
  verifications: WireVerification[];
}

export interface WireFinding extends WireResource {
  id: string;
  inspectionId: string;
  assetCode: string;
  criterion: WireCriterion;
  severity: string;
  responsible: WirePerson;
  open: boolean;
  subsystemCode: string | null;
  evidence: WireAttachedEvidence[];
  actions: WireCorrectiveAction[];
}

export interface WireFindingsSummary extends WireResource {
  assetCode: string;
  findings: {
    findingId: string;
    criterionCode: string;
    description: string;
    severity: string;
    responsible: WirePerson;
    open: boolean;
    overdueActions: number;
    subsystemCode: string | null;
  }[];
  openFindings: number;
}

export type WireScope = { kind: 'WHOLE_ASSET' } | { code: { value: string } };

export type WireBasis =
  | { inspection: { id: { value: string }; closedAt: Instant } }
  | { partials: { value: string }[]; globalPolicy: string; latestInspectionClosedAt: Instant };

export interface WireDecision {
  outcome: CertificationOutcome;
  eligible: boolean;
  ineligibilities: { reason: IneligibilityReason; detail: string }[];
  conditions: { findingId: string; criterionCode: string; description: string; dueDate: LocalDate }[];
  validity: { validFrom: LocalDate; validUntil: LocalDate } | null;
  policyCode: string;
  jurisdictionCode: { value: string };
  evaluatedAt: Instant;
  scope: WireScope;
}

export interface WireEligibility extends WireResource {
  decision: WireDecision;
}

export interface WireSuspension {
  reason: string;
  suspendedBy: WirePerson;
  suspendedAt: Instant;
}

export interface WireCertificate extends WireResource {
  number: string;
  assetCode: string;
  scope: WireScope;
  basis: WireBasis;
  schemaReference: { assetType: { name: string }; number: number };
  validFrom: LocalDate;
  validUntil: LocalDate;
  issuedBy: WirePerson;
  issuedAt: Instant;
  status: CertificateStatus;
  decision: WireDecision;
  suspensions: WireSuspension[];
}

export interface WireCertificateDocument extends WireResource {
  number: string;
  assetCode: string;
  assetName: string;
  scope: WireScope;
  basis: WireBasis;
  validFrom: LocalDate;
  validUntil: LocalDate;
  statusDate: LocalDate;
  status: CertificateStatus;
  decision: WireDecision;
  issuedBy: WirePerson;
  issuedAt: Instant;
  suspensions: WireSuspension[];
}

export interface WireProblem {
  title?: string;
  detail?: string;
  status?: number;
  ineligibilities?: { reason: IneligibilityReason; detail: string }[];
}
