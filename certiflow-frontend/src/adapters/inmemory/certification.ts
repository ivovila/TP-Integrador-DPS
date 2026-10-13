import type {
  Certificate,
  CertificateAction,
  CertificateBasis,
  CertificateModality,
  CertificateStatus,
  CertificationCondition,
  CertificationDecision,
  CertificationScope,
  EligibilityDecision,
  Ineligibility,
} from '../../domain/certification';
import type { Instant, LocalDate, Person } from '../../domain/shared';
import type { CertificationPort } from '../../ports/CertificationPort';
import { storedAssetWithCode } from './catalog';
import { findingsOfAsset, isOpenFinding } from './finding';
import { coversSubsystem, coversWholeSchema, isClosed, storedInspectionWithId } from './inspection';
import { policyFor, type OpenNonConformity } from './policies';
import { storedSchemaFor } from './schema';
import type { CertificateLifecycle, InMemorySystem, StoredCertificate, StoredInspection } from './store';
import { reject, requireText } from './support';

const wholeAsset: CertificationScope = { kind: 'wholeAsset' };
const globalPolicyCode = 'ALL-REQUIRED-SUBSYSTEMS';

function sameScope(first: CertificationScope, second: CertificationScope): boolean {
  return first.kind === 'wholeAsset'
    ? second.kind === 'wholeAsset'
    : second.kind === 'subsystem' && first.subsystemCode === second.subsystemCode;
}

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

/** El estado según el ciclo de vida propio del certificado. */
function statusOn(certificate: StoredCertificate, referenceDate: LocalDate): CertificateStatus {
  return (isWithinValidity(certificate, referenceDate) ? statusWhileValid : statusOnceLapsed)[certificate.lifecycle];
}

function isCurrentOn(certificate: StoredCertificate, referenceDate: LocalDate): boolean {
  return certificate.lifecycle !== 'RENEWED' && isWithinValidity(certificate, referenceDate);
}

function storedCertificateWithNumber(system: InMemorySystem, number: string): StoredCertificate {
  return system.certificates.get(number) ?? reject('CertificateNotFoundException', `Certificate ${number} does not exist`);
}

/** Un global que sería activo deja de serlo en cuanto alguno de sus parciales ya no está activo. */
function effectiveStatusOn(system: InMemorySystem, certificate: StoredCertificate, referenceDate: LocalDate): CertificateStatus {
  const own = statusOn(certificate, referenceDate);
  if (own !== 'ACTIVE' || certificate.basis.kind !== 'partialCertificates') {
    return own;
  }
  const everyBackingActive = certificate.basis.partialCertificateNumbers.every((number) => {
    const partial = system.certificates.get(number);
    return partial !== undefined && statusOn(partial, referenceDate) === 'ACTIVE';
  });
  return everyBackingActive ? own : 'DEPENDENCY_INVALID';
}

function basisView(certificate: StoredCertificate): CertificateBasis {
  return certificate.basis.kind === 'inspection'
    ? { kind: 'inspection', inspectionId: certificate.basis.inspectionId }
    : {
        kind: 'partialCertificates',
        partialCertificateNumbers: [...certificate.basis.partialCertificateNumbers],
        globalPolicy: certificate.basis.globalPolicy,
      };
}

function latestInspectionClosedAt(certificate: StoredCertificate): Instant {
  return certificate.basis.kind === 'inspection'
    ? certificate.basis.inspectionClosedAt
    : certificate.basis.latestInspectionClosedAt;
}

function certificateView(system: InMemorySystem, certificate: StoredCertificate): Certificate {
  const status = effectiveStatusOn(system, certificate, system.clock.today());
  const renewal: CertificateAction = certificate.basis.kind === 'inspection' ? 'renewCertificate' : 'renewGlobalCertificate';
  const actions: CertificateAction[] = [
    ...(status === 'ACTIVE' ? ['suspendCertificate' as const] : []),
    ...(status !== 'RENEWED' ? [renewal] : []),
  ];
  return structuredClone({
    number: certificate.number,
    assetCode: certificate.assetCode,
    scope: certificate.scope,
    basis: basisView(certificate),
    schemaVersion: certificate.schemaVersion,
    requiredSubsystems: certificate.requiredSubsystems,
    validFrom: certificate.validFrom,
    validUntil: certificate.validUntil,
    modality: certificate.decision.outcome as CertificateModality,
    decision: certificate.decision,
    issuedBy: certificate.issuedBy,
    issuedAt: certificate.issuedAt,
    status,
    suspensions: certificate.suspensions,
    actions,
  });
}

/** Hallazgos abiertos del activo que afectan el alcance: los del subsistema y los que no tienen subsistema. */
function openNonConformitiesFor(system: InMemorySystem, assetCode: string, scope: CertificationScope): OpenNonConformity[] {
  return findingsOfAsset(system, assetCode)
    .filter(isOpenFinding)
    .filter(
      (finding) =>
        scope.kind === 'wholeAsset' || finding.subsystemCode === undefined || finding.subsystemCode === scope.subsystemCode,
    )
    .map((finding) => ({
      findingId: finding.id,
      criterionCode: finding.criterion.code,
      severityName: finding.criterion.severity.name,
      severityLabel: finding.criterion.severity.label,
    }));
}

function scopeFor(inspection: StoredInspection, subsystemCode: string | undefined): CertificationScope {
  const requiredSubsystems = inspection.schemaVersion.requiredSubsystems;
  if (subsystemCode === undefined) {
    if (requiredSubsystems.length > 0) {
      reject('CertificationConflictException', 'Global certificates must be derived from partial certificates');
    }
    return wholeAsset;
  }
  if (!requiredSubsystems.includes(subsystemCode)) {
    reject('SubsystemNotFoundException', `Subsystem not found: ${subsystemCode}`);
  }
  return { kind: 'subsystem', subsystemCode };
}

/**
 * Las guardas comunes (inspección cerrada y alcance cubierto) se comprueban antes de la política:
 * ninguna política puede aprobar una inspección abierta o incompleta.
 */
function decisionFor(system: InMemorySystem, inspection: StoredInspection, scope: CertificationScope): CertificationDecision {
  const asset = storedAssetWithCode(system, inspection.assetCode);
  const policy = policyFor(asset.jurisdictionCode);
  const evaluatedAt = system.clock.now();
  const base = { scope, policyCode: policy.code, jurisdictionCode: asset.jurisdictionCode, evaluatedAt };
  const guards: Ineligibility[] = [];
  if (!isClosed(inspection)) {
    guards.push({ reason: 'INSPECTION_NOT_CLOSED', detail: 'Inspection is still open' });
  }
  const covered = scope.kind === 'wholeAsset' ? coversWholeSchema(inspection) : coversSubsystem(inspection, scope.subsystemCode);
  if (!covered) {
    guards.push({ reason: 'PARTIAL_INSPECTION', detail: 'Inspection does not cover certification scope' });
  }
  if (guards.length > 0) {
    return { ...base, outcome: 'REJECTED', eligible: false, ineligibilities: guards, conditions: [] };
  }
  const verdict = policy.evaluate(openNonConformitiesFor(system, inspection.assetCode, scope), system.clock.today());
  return { ...base, ...verdict, eligible: verdict.outcome !== 'REJECTED' };
}

function eligibleDecisionFor(system: InMemorySystem, inspection: StoredInspection, scope: CertificationScope): CertificationDecision {
  const decision = decisionFor(system, inspection, scope);
  if (!decision.eligible) {
    reject('NotEligibleForCertificationException', `Inspection ${inspection.id} cannot back a certificate`, decision.ineligibilities);
  }
  return decision;
}

function currentCertificateFor(
  system: InMemorySystem,
  assetCode: string,
  scope: CertificationScope,
  except?: string,
): StoredCertificate | undefined {
  return [...system.certificates.values()].find(
    (certificate) =>
      certificate.number !== except &&
      certificate.assetCode === assetCode &&
      sameScope(certificate.scope, scope) &&
      isCurrentOn(certificate, system.clock.today()),
  );
}

function ensureScopeIsFree(system: InMemorySystem, assetCode: string, scope: CertificationScope, except?: string): void {
  const occupying = currentCertificateFor(system, assetCode, scope, except);
  if (occupying !== undefined) {
    reject('AssetAlreadyCertifiedException', `Asset ${assetCode} already holds certificate ${occupying.number}`);
  }
}

function store(
  system: InMemorySystem,
  certificate: Omit<StoredCertificate, 'number' | 'validFrom' | 'validUntil' | 'suspensions' | 'lifecycle'>,
): StoredCertificate {
  const stored: StoredCertificate = {
    ...certificate,
    number: `CERT-${system.certificates.size + 1}`,
    validFrom: certificate.decision.validity!.validFrom,
    validUntil: certificate.decision.validity!.validUntil,
    suspensions: [],
    lifecycle: 'ISSUED',
  };
  system.certificates.set(stored.number, stored);
  const decision = stored.decision;
  system.auditLog.record(
    { kind: 'certificate', number: stored.number },
    'ISSUED',
    certificate.issuedBy,
    certificate.issuedAt,
    `Number ${stored.number}, policy ${decision.policyCode}, jurisdiction ${decision.jurisdictionCode}, outcome ${decision.outcome}`,
  );
  return stored;
}

function issueFromInspection(
  system: InMemorySystem,
  inspection: StoredInspection,
  decision: CertificationDecision,
  issuedBy: Person,
): StoredCertificate {
  return store(system, {
    assetCode: inspection.assetCode,
    scope: decision.scope,
    basis: { kind: 'inspection', inspectionId: inspection.id, inspectionClosedAt: inspection.revisions[0]!.sealedAt },
    schemaVersion: inspection.schemaVersion.number,
    requiredSubsystems: [...inspection.schemaVersion.requiredSubsystems],
    decision,
    issuedBy: { ...issuedBy },
    issuedAt: decision.evaluatedAt,
  });
}

function markRenewed(system: InMemorySystem, current: StoredCertificate, renewed: StoredCertificate, renewedBy: Person): void {
  current.lifecycle = 'RENEWED';
  system.auditLog.record(
    { kind: 'certificate', number: current.number },
    'RENEWED',
    renewedBy,
    renewed.issuedAt,
    `Renewed by ${renewed.number}`,
  );
}

function minimumOf(dates: LocalDate[]): LocalDate {
  return dates.reduce((earliest, date) => (date < earliest ? date : earliest));
}

/**
 * `AllRequiredSubsystemsPolicy`: un parcial activo por cada subsistema requerido, del mismo activo, versión y jurisdicción.
 * El global rige desde hoy hasta el menor vencimiento de sus parciales y hereda sus condiciones.
 */
function globalDecisionFor(
  system: InMemorySystem,
  assetCode: string,
  schemaVersion: number,
  requiredSubsystems: string[],
  partials: StoredCertificate[],
  jurisdictionCode: string,
): CertificationDecision {
  const today = system.clock.today();
  const base = { scope: wholeAsset, policyCode: globalPolicyCode, jurisdictionCode, evaluatedAt: system.clock.now() };
  const ineligibilities: Ineligibility[] = [];
  const seen = new Set<string>();
  if (requiredSubsystems.length === 0) {
    ineligibilities.push({ reason: 'MISSING_SUBSYSTEM', detail: 'Schema has no required subsystems' });
  }
  partials.forEach((partial) => {
    const subsystemCode = partial.scope.kind === 'subsystem' ? partial.scope.subsystemCode : undefined;
    const backsARequiredSubsystem =
      partial.assetCode === assetCode &&
      partial.schemaVersion === schemaVersion &&
      subsystemCode !== undefined &&
      requiredSubsystems.includes(subsystemCode) &&
      !seen.has(subsystemCode);
    if (backsARequiredSubsystem) {
      seen.add(subsystemCode);
    }
    const valid =
      backsARequiredSubsystem &&
      partial.decision.jurisdictionCode === jurisdictionCode &&
      statusOn(partial, today) === 'ACTIVE';
    if (!valid) {
      ineligibilities.push({ reason: 'INVALID_PARTIAL_CERTIFICATE', detail: `Invalid backing ${partial.number}` });
    }
  });
  [...requiredSubsystems]
    .sort()
    .filter((code) => !seen.has(code))
    .forEach((code) => ineligibilities.push({ reason: 'MISSING_SUBSYSTEM', detail: `Missing partial for ${code}` }));
  if (ineligibilities.length > 0) {
    return { ...base, outcome: 'REJECTED', eligible: false, ineligibilities, conditions: [] };
  }
  const validUntil = minimumOf(partials.map((partial) => partial.validUntil));
  const conditions: CertificationCondition[] = partials.flatMap((partial) =>
    partial.decision.conditions.map((condition) => ({
      ...condition,
      dueDate: condition.dueDate > validUntil ? validUntil : condition.dueDate,
    })),
  );
  return {
    ...base,
    outcome: conditions.length === 0 ? 'APPROVED' : 'CONDITIONAL',
    eligible: true,
    ineligibilities: [],
    conditions,
    validity: { validFrom: today, validUntil },
  };
}

function issueGlobal(
  system: InMemorySystem,
  assetCode: string,
  schemaVersion: number,
  partialCertificateNumbers: string[],
  issuedBy: Person,
  renewing?: StoredCertificate,
): string {
  const asset = storedAssetWithCode(system, assetCode);
  const version =
    storedSchemaFor(system, asset.type).versions.find((candidate) => candidate.number === schemaVersion) ??
    reject('CertificationConflictException', 'Schema version not found');
  const partials = partialCertificateNumbers.map((number) => storedCertificateWithNumber(system, number));
  const decision = globalDecisionFor(system, assetCode, schemaVersion, version.requiredSubsystems, partials, asset.jurisdictionCode);
  if (!decision.eligible) {
    reject('NotEligibleForCertificationException', `Asset ${assetCode} cannot hold a global certificate`, decision.ineligibilities);
  }
  const latest = partials.map(latestInspectionClosedAt).reduce((max, instant) => (instant > max ? instant : max));
  if (renewing !== undefined) {
    const renewable =
      renewing.basis.kind === 'partialCertificates' &&
      renewing.assetCode === assetCode &&
      latest > latestInspectionClosedAt(renewing) &&
      renewing.lifecycle !== 'RENEWED';
    if (!renewable) {
      reject('CertificationConflictException', 'Global renewal requires matching asset and later inspection basis');
    }
  }
  ensureScopeIsFree(system, assetCode, wholeAsset, renewing?.number);
  const global = store(system, {
    assetCode,
    scope: wholeAsset,
    basis: {
      kind: 'partialCertificates',
      partialCertificateNumbers: [...partialCertificateNumbers],
      globalPolicy: globalPolicyCode,
      latestInspectionClosedAt: latest,
    },
    schemaVersion,
    requiredSubsystems: [...version.requiredSubsystems],
    decision,
    issuedBy: { ...issuedBy },
    issuedAt: decision.evaluatedAt,
  });
  if (renewing !== undefined) {
    markRenewed(system, renewing, global, issuedBy);
  }
  return global.number;
}

export function createCertificationPort(system: InMemorySystem): CertificationPort {
  return {
    async evaluateCertification(inspectionId, subsystemCode) {
      const inspection = storedInspectionWithId(system, inspectionId);
      const decision = decisionFor(system, inspection, scopeFor(inspection, subsystemCode));
      const eligibility: EligibilityDecision = {
        ...decision,
        inspectionId: inspection.id,
        actions: decision.eligible ? ['issueCertificate'] : [],
      };
      return structuredClone(eligibility);
    },

    async issueCertificate(command) {
      const inspection = storedInspectionWithId(system, command.inspectionId);
      const decision = eligibleDecisionFor(system, inspection, scopeFor(inspection, command.subsystemCode));
      ensureScopeIsFree(system, inspection.assetCode, decision.scope);
      return issueFromInspection(system, inspection, decision, command.issuedBy).number;
    },

    async issueGlobalCertificate(command) {
      return issueGlobal(system, command.assetCode, command.schemaVersion, command.partialCertificateNumbers, command.issuedBy);
    },

    async listCertificates(filter) {
      return [...system.certificates.values()]
        .filter((certificate) => filter.assetCode === undefined || certificate.assetCode === filter.assetCode)
        .filter(
          (certificate) =>
            filter.subsystemCode === undefined ||
            (certificate.scope.kind === 'subsystem' && certificate.scope.subsystemCode === filter.subsystemCode),
        )
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
      if (currentCertificate.basis.kind !== 'inspection') {
        reject('CertificationConflictException', 'Global certificates are renewed from partial certificates');
      }
      const inspection = storedInspectionWithId(system, command.inspectionId);
      if (inspection.assetCode !== currentCertificate.assetCode) {
        reject(
          'InspectionOfAnotherAssetException',
          `Inspection ${inspection.id} does not belong to asset ${currentCertificate.assetCode}`,
        );
      }
      const scope = currentCertificate.scope;
      const decision = eligibleDecisionFor(
        system,
        inspection,
        scopeFor(inspection, scope.kind === 'subsystem' ? scope.subsystemCode : undefined),
      );
      if (currentCertificate.lifecycle === 'RENEWED') {
        reject('CertificateAlreadyRenewedException', `Certificate ${currentCertificate.number} was already renewed`);
      }
      if (inspection.revisions[0]!.sealedAt <= latestInspectionClosedAt(currentCertificate)) {
        reject(
          'RenewalRequiresLaterInspectionException',
          `Certificate ${currentCertificate.number} can only be renewed with an inspection closed after the one it is based on`,
        );
      }
      ensureScopeIsFree(system, inspection.assetCode, scope, currentCertificate.number);
      const renewedCertificate = issueFromInspection(system, inspection, decision, command.renewedBy);
      markRenewed(system, currentCertificate, renewedCertificate, command.renewedBy);
      return renewedCertificate.number;
    },

    async renewGlobalCertificate(command) {
      const currentCertificate = storedCertificateWithNumber(system, command.currentNumber);
      return issueGlobal(
        system,
        currentCertificate.assetCode,
        command.schemaVersion,
        command.partialCertificateNumbers,
        command.renewedBy,
        currentCertificate,
      );
    },

    async generateCertificateDocument(number) {
      const certificate = certificateView(system, storedCertificateWithNumber(system, number));
      const { actions, schemaVersion, requiredSubsystems, ...documentData } = certificate;
      return {
        ...documentData,
        assetName: storedAssetWithCode(system, certificate.assetCode).name,
        statusDate: system.clock.today(),
      };
    },
  };
}
