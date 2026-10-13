import type {
  Certificate,
  CertificateBasis,
  CertificateDocument,
  CertificateModality,
  CertificationDecision,
  CertificationScope,
  EligibilityDecision,
} from '../../domain/certification';
import { actionsOfferedBy, certificateActionByRel, eligibilityActionByRel } from './actions';
import type { WireBasis, WireCertificate, WireCertificateDocument, WireDecision, WireEligibility, WireScope } from './wire';

const scopeByType: Record<WireScope['type'], (wire: WireScope) => CertificationScope> = {
  WHOLE_ASSET: () => ({ kind: 'wholeAsset' }),
  SUBSYSTEM: (wire) => ({ kind: 'subsystem', subsystemCode: wire.subsystemCode! }),
};

const basisByType: Record<WireBasis['type'], (wire: WireBasis) => CertificateBasis> = {
  INSPECTION: (wire) => ({ kind: 'inspection', inspectionId: wire.inspectionId! }),
  PARTIAL_CERTIFICATES: (wire) => ({
    kind: 'partialCertificates',
    partialCertificateNumbers: wire.partialCertificateNumbers,
    globalPolicy: wire.globalPolicy!,
  }),
};

export function scopeOf(wire: WireScope): CertificationScope {
  return scopeByType[wire.type](wire);
}

export function basisOf(wire: WireBasis): CertificateBasis {
  return basisByType[wire.type](wire);
}

export function decisionOf(wire: WireDecision): CertificationDecision {
  return {
    scope: scopeOf(wire.scope),
    outcome: wire.outcome,
    eligible: wire.eligible,
    ineligibilities: wire.reasons,
    conditions: wire.conditions,
    ...(wire.validFrom === null || wire.validUntil === null ? {} : { validity: { validFrom: wire.validFrom, validUntil: wire.validUntil } }),
    policyCode: wire.policyCode,
    jurisdictionCode: wire.jurisdictionCode,
    evaluatedAt: wire.evaluatedAt,
  };
}

export function eligibilityDecisionOf(wire: WireEligibility, inspectionId: string): EligibilityDecision {
  return { ...decisionOf(wire.decision), inspectionId, actions: actionsOfferedBy(wire, eligibilityActionByRel) };
}

function modalityOf(wire: WireDecision): CertificateModality {
  return wire.outcome === 'CONDITIONAL' ? 'CONDITIONAL' : 'APPROVED';
}

export function certificateOf(wire: WireCertificate, requiredSubsystems: string[]): Certificate {
  return {
    number: wire.number,
    assetCode: wire.assetCode,
    scope: scopeOf(wire.scope),
    basis: basisOf(wire.basis),
    schemaVersion: wire.schemaReference.number,
    requiredSubsystems,
    validFrom: wire.validFrom,
    validUntil: wire.validUntil,
    modality: modalityOf(wire.decision),
    decision: decisionOf(wire.decision),
    issuedBy: wire.issuedBy,
    issuedAt: wire.issuedAt,
    status: wire.status,
    suspensions: wire.suspensions,
    actions: actionsOfferedBy(wire, certificateActionByRel),
  };
}

export function certificateDocumentOf(wire: WireCertificateDocument): CertificateDocument {
  return {
    number: wire.number,
    assetCode: wire.assetCode,
    assetName: wire.assetName,
    scope: scopeOf(wire.scope),
    basis: basisOf(wire.basis),
    validFrom: wire.validFrom,
    validUntil: wire.validUntil,
    modality: modalityOf(wire.decision),
    decision: decisionOf(wire.decision),
    statusDate: wire.statusDate,
    status: wire.status,
    issuedBy: wire.issuedBy,
    issuedAt: wire.issuedAt,
    suspensions: wire.suspensions,
  };
}
