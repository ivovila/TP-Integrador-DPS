import type {
  Certificate,
  CertificateAction,
  CertificateStatus,
  CertificationDecision,
  Ineligibility,
} from '../../domain/certification';
import type { LocalDate, Person } from '../../domain/shared';
import type { CertificationPort } from '../../ports/CertificationPort';
import { storedAssetWithCode } from './catalog';
import { blocksCertification, findingsOfAsset } from './finding';
import { coversWholeSchema, isClosed, storedInspectionWithId } from './inspection';
import type { CertificateLifecycle, InMemorySystem, StoredCertificate, StoredInspection } from './store';
import { reject, requireText } from './support';

function isWithinValidity(certificate: StoredCertificate, referenceDate: LocalDate): boolean {
  return referenceDate >= certificate.validFrom && referenceDate <= certificate.validUntil;
}

const statusWhileValid: Record<CertificateLifecycle, CertificateStatus> = {
  ISSUED: 'ACTIVE',
  SUSPENDED: 'SUSPENDED',
  RENEWED: 'RENEWED',
};

const statusOnceLapsed: Record<CertificateLifecycle, CertificateStatus> = {
  ISSUED: 'EXPIRED',
  SUSPENDED: 'EXPIRED',
  RENEWED: 'RENEWED',
};

function statusOn(certificate: StoredCertificate, referenceDate: LocalDate): CertificateStatus {
  return (isWithinValidity(certificate, referenceDate) ? statusWhileValid : statusOnceLapsed)[certificate.lifecycle];
}

function isCurrentOn(certificate: StoredCertificate, referenceDate: LocalDate): boolean {
  return certificate.lifecycle !== 'RENEWED' && isWithinValidity(certificate, referenceDate);
}

function storedCertificateWithNumber(system: InMemorySystem, number: string): StoredCertificate {
  return system.certificates.get(number) ?? reject('CertificateNotFoundException', `Certificate ${number} does not exist`);
}

function certificateView(system: InMemorySystem, certificate: StoredCertificate): Certificate {
  const { inspectionClosedAt, lifecycle, ...certificateData } = certificate;
  const status = statusOn(certificate, system.clock.today());
  const actions: CertificateAction[] = [
    ...(status === 'ACTIVE' ? ['suspendCertificate' as const] : []),
    ...(status !== 'RENEWED' ? ['renewCertificate' as const] : []),
  ];
  return structuredClone({ ...certificateData, status, actions });
}

function decisionFor(system: InMemorySystem, inspection: StoredInspection): CertificationDecision {
  const ineligibilities: Ineligibility[] = [];
  if (!isClosed(inspection)) {
    ineligibilities.push({ reason: 'INSPECTION_NOT_CLOSED', detail: `Inspection ${inspection.id} is still open` });
  }
  if (!coversWholeSchema(inspection)) {
    ineligibilities.push({
      reason: 'PARTIAL_INSPECTION',
      detail: 'Only an inspection that covers the whole schema can back a certificate',
    });
  }
  const blockingFindings = findingsOfAsset(system, inspection.assetCode).filter(blocksCertification).length;
  if (blockingFindings > 0) {
    ineligibilities.push({
      reason: 'BLOCKING_FINDINGS',
      detail: `Asset ${inspection.assetCode} has ${blockingFindings} open finding(s) whose severity blocks certification`,
    });
  }
  const eligible = ineligibilities.length === 0;
  return { inspectionId: inspection.id, eligible, ineligibilities, actions: eligible ? ['issueCertificate'] : [] };
}

function ensureEligible(system: InMemorySystem, inspection: StoredInspection): void {
  const decision = decisionFor(system, inspection);
  if (!decision.eligible) {
    reject(
      'NotEligibleForCertificationException',
      `Inspection ${inspection.id} cannot back a certificate`,
      decision.ineligibilities,
    );
  }
}

function ensureValidityStartsToday(system: InMemorySystem, validUntil: LocalDate): void {
  requireText(validUntil, 'A certificate needs an expiry date');
  if (validUntil < system.clock.today()) {
    reject('InvalidValidityPeriodException', `The validity cannot end on ${validUntil}, before it starts on ${system.clock.today()}`);
  }
}

function issue(
  system: InMemorySystem,
  inspection: StoredInspection,
  validUntil: LocalDate,
  issuedBy: Person,
): StoredCertificate {
  const issuedAt = system.clock.now();
  const certificate: StoredCertificate = {
    number: `CERT-${system.certificates.size + 1}`,
    assetCode: inspection.assetCode,
    inspectionId: inspection.id,
    inspectionClosedAt: inspection.revisions[0]!.sealedAt,
    validFrom: system.clock.today(),
    validUntil,
    issuedBy: { ...issuedBy },
    issuedAt,
    suspensions: [],
    lifecycle: 'ISSUED',
  };
  system.certificates.set(certificate.number, certificate);
  system.auditLog.record(
    { kind: 'certificate', number: certificate.number },
    'ISSUED',
    issuedBy,
    issuedAt,
    `Number ${certificate.number}`,
  );
  return certificate;
}

export function createCertificationPort(system: InMemorySystem): CertificationPort {
  return {
    async evaluateCertification(inspectionId) {
      return decisionFor(system, storedInspectionWithId(system, inspectionId));
    },

    async issueCertificate(command) {
      const inspection = storedInspectionWithId(system, command.inspectionId);
      ensureEligible(system, inspection);
      const currentCertificate = [...system.certificates.values()].find(
        (certificate) => certificate.assetCode === inspection.assetCode && isCurrentOn(certificate, system.clock.today()),
      );
      if (currentCertificate !== undefined) {
        reject(
          'AssetAlreadyCertifiedException',
          `Asset ${inspection.assetCode} already holds certificate ${currentCertificate.number}`,
        );
      }
      ensureValidityStartsToday(system, command.validUntil);
      return issue(system, inspection, command.validUntil, command.issuedBy).number;
    },

    async listCertificates(filter) {
      return [...system.certificates.values()]
        .filter((certificate) => filter.assetCode === undefined || certificate.assetCode === filter.assetCode)
        .map((certificate) => certificateView(system, certificate));
    },

    async findCertificate(number) {
      return certificateView(system, storedCertificateWithNumber(system, number));
    },

    async suspendCertificate(command) {
      const certificate = storedCertificateWithNumber(system, command.number);
      requireText(command.reason, 'A suspension must explain its reason');
      if (statusOn(certificate, system.clock.today()) !== 'ACTIVE') {
        reject('CertificateNotActiveException', `Certificate ${certificate.number} is not active`);
      }
      const suspendedAt = system.clock.now();
      certificate.suspensions.push({ reason: command.reason, suspendedBy: { ...command.suspendedBy }, suspendedAt });
      certificate.lifecycle = 'SUSPENDED';
      system.auditLog.record(
        { kind: 'certificate', number: certificate.number },
        'SUSPENDED',
        command.suspendedBy,
        suspendedAt,
        command.reason,
      );
    },

    async renewCertificate(command) {
      const currentCertificate = storedCertificateWithNumber(system, command.currentNumber);
      const inspection = storedInspectionWithId(system, command.inspectionId);
      ensureEligible(system, inspection);
      ensureValidityStartsToday(system, command.validUntil);
      if (currentCertificate.lifecycle === 'RENEWED') {
        reject('CertificateAlreadyRenewedException', `Certificate ${currentCertificate.number} was already renewed`);
      }
      if (inspection.assetCode !== currentCertificate.assetCode) {
        reject(
          'InspectionOfAnotherAssetException',
          `Inspection ${inspection.id} does not belong to asset ${currentCertificate.assetCode}`,
        );
      }
      if (inspection.revisions[0]!.sealedAt <= currentCertificate.inspectionClosedAt) {
        reject(
          'RenewalRequiresLaterInspectionException',
          `Certificate ${currentCertificate.number} can only be renewed with an inspection closed after the one it is based on`,
        );
      }
      const renewedCertificate = issue(system, inspection, command.validUntil, command.renewedBy);
      currentCertificate.lifecycle = 'RENEWED';
      system.auditLog.record(
        { kind: 'certificate', number: currentCertificate.number },
        'RENEWED',
        command.renewedBy,
        renewedCertificate.issuedAt,
        `Renewed by ${renewedCertificate.number}`,
      );
      return renewedCertificate.number;
    },

    async generateCertificateDocument(number) {
      const certificate = certificateView(system, storedCertificateWithNumber(system, number));
      const { inspectionId, actions, ...documentData } = certificate;
      return {
        ...documentData,
        assetName: storedAssetWithCode(system, certificate.assetCode).name,
        statusDate: system.clock.today(),
      };
    },
  };
}
