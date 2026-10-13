import type { AuditEntry, AuditSubject } from '../domain/audit';

export interface AuditPort {
  auditHistory(subject: AuditSubject): Promise<AuditEntry[]>;
}
