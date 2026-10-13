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

export function scopeOf(wire: WireScope): CertificationScope {
  return 'code' in wire ? { kind: 'subsystem', subsystemCode: wire.code.value } : { kind: 'wholeAsset' };
}

export function basisOf(wire: WireBasis): CertificateBasis {
  return 'inspection' in wire
    ? { kind: 'inspection', inspectionId: wire.inspection.id.value }
    : {
        kind: 'partialCertificates',
        partialCertificateNumbers: wire.partials.map((partial) => partial.value),
        globalPolicy: wire.globalPolicy,
      };
}

export function decisionOf(wire: WireDecision): CertificationDecision {
  return {
    scope: scopeOf(wire.scope),
    outcome: wire.outcome,
    eligible: wire.eligible,
    ineligibilities: wire.ineligibilities,
    conditions: wire.conditions,
    ...(wire.validity === null ? {} : { validity: wire.validity }),
    policyCode: wire.policyCode,
    jurisdictionCode: wire.jurisdictionCode.value,
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
