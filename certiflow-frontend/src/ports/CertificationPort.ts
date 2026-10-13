import type { Certificate, CertificateDocument, EligibilityDecision } from '../domain/certification';
import type { Person } from '../domain/shared';

export interface IssueCertificateCommand {
  inspectionId: string;
  /** Con subsistema se emite un parcial; sin él, un certificado del activo entero (solo si el esquema no tiene subsistemas). */
  subsystemCode?: string;
  issuedBy: Person;
}

export interface SuspendCertificateCommand {
  number: string;
  reason: string;
  suspendedBy: Person;
}

/** Renovar conserva el alcance del certificado actual. */
export interface RenewCertificateCommand {
  currentNumber: string;
  inspectionId: string;
  renewedBy: Person;
}

export interface IssueGlobalCertificateCommand {
  assetCode: string;
  schemaVersion: number;
  partialCertificateNumbers: string[];
  issuedBy: Person;
}

export interface RenewGlobalCertificateCommand {
  currentNumber: string;
  schemaVersion: number;
  partialCertificateNumbers: string[];
  renewedBy: Person;
}

export interface CertificateFilter {
  assetCode?: string;
  subsystemCode?: string;
}

export interface CertificationPort {
  evaluateCertification(inspectionId: string, subsystemCode?: string): Promise<EligibilityDecision>;
  issueCertificate(command: IssueCertificateCommand): Promise<string>;
  issueGlobalCertificate(command: IssueGlobalCertificateCommand): Promise<string>;
  listCertificates(filter: CertificateFilter): Promise<Certificate[]>;
  findCertificate(number: string): Promise<Certificate>;
  suspendCertificate(command: SuspendCertificateCommand): Promise<void>;
  renewCertificate(command: RenewCertificateCommand): Promise<string>;
  renewGlobalCertificate(command: RenewGlobalCertificateCommand): Promise<string>;
  generateCertificateDocument(number: string): Promise<CertificateDocument>;
}
