import type { Instant, LocalDate, Person } from './shared';

export type CertificateStatus = 'ACTIVE' | 'EXPIRED' | 'SUSPENDED' | 'RENEWED' | 'DEPENDENCY_INVALID';
export type CertificationOutcome = 'APPROVED' | 'CONDITIONAL' | 'REJECTED';
export type CertificateModality = Exclude<CertificationOutcome, 'REJECTED'>;
export type IneligibilityReason =
  | 'INSPECTION_NOT_CLOSED'
  | 'PARTIAL_INSPECTION'
  | 'BLOCKING_FINDINGS'
  | 'UNSUPPORTED_SEVERITY'
  | 'MISSING_SUBSYSTEM'
  | 'INVALID_PARTIAL_CERTIFICATE'
  | 'REQUIRES_PARTIAL_CERTIFICATES';

export interface Ineligibility {
  reason: IneligibilityReason;
  detail: string;
}

export type CertificationScope = { kind: 'wholeAsset' } | { kind: 'subsystem'; subsystemCode: string };

export type CertificateBasis =
  | { kind: 'inspection'; inspectionId: string }
  | { kind: 'partialCertificates'; partialCertificateNumbers: string[]; globalPolicy: string };

export interface CertificationCondition {
  findingId: string;
  criterionCode: string;
  description: string;
  dueDate: LocalDate;
}

export interface ValidityPeriod {
  validFrom: LocalDate;
  validUntil: LocalDate;
}

export type CertificationDecisionAction = 'issueCertificate';

/** Lo que decidió la política de la jurisdicción del activo; la vigencia la fija la política, no quien emite. */
export interface CertificationDecision {
  scope: CertificationScope;
  outcome: CertificationOutcome;
  eligible: boolean;
  ineligibilities: Ineligibility[];
  conditions: CertificationCondition[];
  validity?: ValidityPeriod;
  policyCode: string;
  jurisdictionCode: string;
  evaluatedAt: Instant;
}

export interface EligibilityDecision extends CertificationDecision {
  inspectionId: string;
  actions: CertificationDecisionAction[];
}

export interface Suspension {
  reason: string;
  suspendedBy: Person;
  suspendedAt: Instant;
}

export type CertificateAction = 'suspendCertificate' | 'renewCertificate' | 'renewGlobalCertificate';

export interface Certificate {
  number: string;
  assetCode: string;
  scope: CertificationScope;
  basis: CertificateBasis;
  schemaVersion: number;
  requiredSubsystems: string[];
  validFrom: LocalDate;
  validUntil: LocalDate;
  modality: CertificateModality;
  decision: CertificationDecision;
  issuedBy: Person;
  issuedAt: Instant;
  status: CertificateStatus;
  suspensions: Suspension[];
  actions: CertificateAction[];
}

export interface CertificateDocument {
  number: string;
  assetCode: string;
  assetName: string;
  scope: CertificationScope;
  basis: CertificateBasis;
  validFrom: LocalDate;
  validUntil: LocalDate;
  modality: CertificateModality;
  decision: CertificationDecision;
  statusDate: LocalDate;
  status: CertificateStatus;
  issuedBy: Person;
  issuedAt: Instant;
  suspensions: Suspension[];
}
