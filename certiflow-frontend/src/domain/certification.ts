import type { Instant, LocalDate, Person } from './shared';

export type CertificateStatus = 'ACTIVE' | 'EXPIRED' | 'SUSPENDED' | 'RENEWED';
export type IneligibilityReason = 'INSPECTION_NOT_CLOSED' | 'PARTIAL_INSPECTION' | 'BLOCKING_FINDINGS';

export interface Ineligibility {
  reason: IneligibilityReason;
  detail: string;
}

export type CertificationDecisionAction = 'issueCertificate';

export interface CertificationDecision {
  inspectionId: string;
  eligible: boolean;
  ineligibilities: Ineligibility[];
  actions: CertificationDecisionAction[];
}

export interface Suspension {
  reason: string;
  suspendedBy: Person;
  suspendedAt: Instant;
}

export type CertificateAction = 'suspendCertificate' | 'renewCertificate';

export interface Certificate {
  number: string;
  assetCode: string;
  inspectionId: string;
  validFrom: LocalDate;
  validUntil: LocalDate;
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
  validFrom: LocalDate;
  validUntil: LocalDate;
  statusDate: LocalDate;
  status: CertificateStatus;
  issuedBy: Person;
  issuedAt: Instant;
  suspensions: Suspension[];
}
