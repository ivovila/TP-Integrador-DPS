import type { AuditEntry } from '../../domain/audit';
import type { WireAuditEntry } from './wire';

export function auditEntryOf(wire: WireAuditEntry): AuditEntry {
  return {
    action: wire.action,
    label: wire.label,
    performedBy: { id: wire.performedById, name: wire.performedByName },
    occurredAt: wire.occurredAt,
    detail: wire.detail,
  };
}
