import type { AuditSubject } from '../../domain/audit';
import type { AuditPort } from '../../ports/AuditPort';
import { auditEntryOf } from './auditMapping';
import { type HalClient, segment } from './HalClient';
import type { WireAuditEntry } from './wire';

const subjectPath: { [Kind in AuditSubject['kind']]: (subject: Extract<AuditSubject, { kind: Kind }>) => string } = {
  asset: (subject) => `/api/v1/assets/${segment(subject.code)}`,
  schema: (subject) => `/api/v1/schemas/${segment(subject.assetType)}`,
  inspection: (subject) => `/api/v1/inspections/${segment(subject.id)}`,
  finding: (subject) => `/api/v1/findings/${segment(subject.id)}`,
  certificate: (subject) => `/api/v1/certificates/${segment(subject.number)}`,
};

export function createHttpAuditPort(client: HalClient): AuditPort {
  return {
    async auditHistory(subject) {
      const pathOfSubject = subjectPath[subject.kind] as (subject: AuditSubject) => string;
      const entries = await client.readAll<WireAuditEntry>(`${pathOfSubject(subject)}/audit-entries`, 'auditEntries');
      return entries.map(auditEntryOf);
    },
  };
}
