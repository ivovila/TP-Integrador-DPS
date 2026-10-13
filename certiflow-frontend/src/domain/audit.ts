import type { Instant, Person } from './shared';

export type AuditSubject =
  | { kind: 'asset'; code: string }
  | { kind: 'schema'; assetType: string }
  | { kind: 'inspection'; id: string }
  | { kind: 'finding'; id: string }
  | { kind: 'certificate'; number: string };

export interface AuditEntry {
  action: string;
  label: string;
  performedBy: Person;
  occurredAt: Instant;
  detail: string;
}
