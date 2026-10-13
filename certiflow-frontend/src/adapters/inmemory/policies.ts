import type {
  CertificationCondition,
  CertificationOutcome,
  Ineligibility,
  ValidityPeriod,
} from '../../domain/certification';
import type { Jurisdiction } from '../../domain/schema';
import type { LocalDate } from '../../domain/shared';
import { reject } from './support';

/** Un hallazgo abierto que pesa sobre el alcance que se certifica. */
export interface OpenNonConformity {
  findingId: string;
  criterionCode: string;
  severityName: string;
  severityLabel: string;
}

export interface PolicyVerdict {
  outcome: CertificationOutcome;
  ineligibilities: Ineligibility[];
  conditions: CertificationCondition[];
  validity?: ValidityPeriod;
}

/** Estrategia de certificación de una jurisdicción, como `CertificationPolicy` del backend. */
export interface CertificationPolicy {
  code: string;
  evaluate(openNonConformities: OpenNonConformity[], issuanceDate: LocalDate): PolicyVerdict;
}

function plusDays(date: LocalDate, days: number): LocalDate {
  const shifted = new Date(`${date}T00:00:00Z`);
  shifted.setUTCDate(shifted.getUTCDate() + days);
  return shifted.toISOString().slice(0, 10);
}

/** Igual que `LocalDate.plusMonths` de Java: si el día no existe en el mes destino, queda en el último. */
function plusMonths(date: LocalDate, months: number): LocalDate {
  const [year, month, day] = date.split('-').map(Number) as [number, number, number];
  const target = new Date(Date.UTC(year, month - 1 + months, 1));
  const lastDayOfTarget = new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth() + 1, 0)).getUTCDate();
  target.setUTCDate(Math.min(day, lastDayOfTarget));
  return target.toISOString().slice(0, 10);
}

/** Un plazo de N meses incluye ambos extremos: termina el día anterior al mismo día N meses después. */
export function monthsOfValidity(from: LocalDate, months: number): ValidityPeriod {
  return { validFrom: from, validUntil: plusDays(plusMonths(from, months), -1) };
}

/** Un plazo de N días incluye ambos extremos. */
export function daysOfValidity(from: LocalDate, days: number): ValidityPeriod {
  return { validFrom: from, validUntil: plusDays(from, days - 1) };
}

function describe(finding: OpenNonConformity): string {
  return `Open finding ${finding.criterionCode} severity ${finding.severityLabel}`;
}

function rejected(ineligibilities: Ineligibility[]): PolicyVerdict {
  return { outcome: 'REJECTED', ineligibilities, conditions: [] };
}

function approved(validity: ValidityPeriod): PolicyVerdict {
  return { outcome: 'APPROVED', ineligibilities: [], conditions: [], validity };
}

const standardSeverityNames = ['MINOR', 'MAJOR', 'CRITICAL'];

function blockingReason(finding: OpenNonConformity): Ineligibility {
  return {
    reason: standardSeverityNames.includes(finding.severityName) ? 'BLOCKING_FINDINGS' : 'UNSUPPORTED_SEVERITY',
    detail: describe(finding),
  };
}

/** Cualquier hallazgo abierto rechaza; aprueba por 12 meses. */
const strictPolicy: CertificationPolicy = {
  code: 'STRICT',
  evaluate(openNonConformities, issuanceDate) {
    return openNonConformities.length > 0
      ? rejected(openNonConformities.map(blockingReason))
      : approved(monthsOfValidity(issuanceDate, 12));
  },
};

/** MAJOR y CRITICAL rechazan; los MINOR no impiden aprobar por 12 meses. */
const standardPolicy: CertificationPolicy = {
  code: 'STANDARD',
  evaluate(openNonConformities, issuanceDate) {
    const blocking = openNonConformities.filter((finding) => finding.severityName !== 'MINOR');
    return blocking.length > 0 ? rejected(blocking.map(blockingReason)) : approved(monthsOfValidity(issuanceDate, 12));
  },
};

/** CRITICAL rechaza; MINOR o MAJOR abiertos dan un certificado condicional por 30 días; sin hallazgos aprueba por 6 meses. */
const conditionalPolicy: CertificationPolicy = {
  code: 'CONDITIONAL',
  evaluate(openNonConformities, issuanceDate) {
    const blocking = openNonConformities.filter((finding) => !['MINOR', 'MAJOR'].includes(finding.severityName));
    if (blocking.length > 0) {
      return rejected(blocking.map(blockingReason));
    }
    if (openNonConformities.length === 0) {
      return approved(monthsOfValidity(issuanceDate, 6));
    }
    const validity = daysOfValidity(issuanceDate, 30);
    return {
      outcome: 'CONDITIONAL',
      ineligibilities: [],
      conditions: openNonConformities.map((finding) => ({
        findingId: finding.findingId,
        criterionCode: finding.criterionCode,
        description: 'Resolve and verify open nonconformity',
        dueDate: validity.validUntil,
      })),
      validity,
    };
  },
};

/** Registro jurisdicción → política, el mismo que arma `UseCaseConfiguration` en el backend. */
const policyByJurisdiction: Record<string, CertificationPolicy> = {
  'DEMO-STRICT': strictPolicy,
  'DEMO-CONDITIONAL': conditionalPolicy,
  'DEMO-STANDARD': standardPolicy,
};

export const demoJurisdictions: Jurisdiction[] = [
  { code: 'DEMO-STRICT', policyCode: 'STRICT' },
  { code: 'DEMO-CONDITIONAL', policyCode: 'CONDITIONAL' },
  { code: 'DEMO-STANDARD', policyCode: 'STANDARD' },
];

export function isKnownJurisdiction(jurisdictionCode: string): boolean {
  return policyByJurisdiction[jurisdictionCode] !== undefined;
}

export function policyFor(jurisdictionCode: string): CertificationPolicy {
  return (
    policyByJurisdiction[jurisdictionCode] ??
    reject('JurisdictionPolicyNotFoundException', `No certification policy configured for ${jurisdictionCode}`)
  );
}
