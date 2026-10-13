import type { Certificate, CertificateDocument, CertificationDecision } from '../domain/certification';
import type { LocalDate, Person } from '../domain/shared';

export interface IssueCertificateCommand {
  inspectionId: string;
  validUntil: LocalDate;
  issuedBy: Person;
}

export interface SuspendCertificateCommand {
  number: string;
  reason: string;
  suspendedBy: Person;
}

export interface RenewCertificateCommand {
  currentNumber: string;
  inspectionId: string;
  validUntil: LocalDate;
  renewedBy: Person;
}

export interface CertificateFilter {
  assetCode?: string;
}

export interface CertificationPort {
  evaluateCertification(inspectionId: string): Promise<CertificationDecision>;
  issueCertificate(command: IssueCertificateCommand): Promise<string>;
  listCertificates(filter: CertificateFilter): Promise<Certificate[]>;
  findCertificate(number: string): Promise<Certificate>;
  suspendCertificate(command: SuspendCertificateCommand): Promise<void>;
  renewCertificate(command: RenewCertificateCommand): Promise<string>;
  generateCertificateDocument(number: string): Promise<CertificateDocument>;
}
