import type { AuditEntry, AuditSubject } from '../../domain/audit';
import { useCertiflow } from '../app/CertiflowContext';
import { formatInstant, formatPerson } from './format';
import { QueryView } from './QueryView';
import { useQuery } from './useQuery';

export function AuditEntriesTable({ entries }: { entries: AuditEntry[] }) {
  if (entries.length === 0) {
    return <p className="muted">Nothing recorded yet.</p>;
  }
  return (
    <table className="data">
      <thead>
        <tr>
          <th>When</th>
          <th>What</th>
          <th>Who</th>
          <th>Detail</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((entry, position) => (
          <tr key={position}>
            <td>{formatInstant(entry.occurredAt)}</td>
            <td>{entry.label}</td>
            <td>{formatPerson(entry.performedBy)}</td>
            <td>{entry.detail}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export function AuditHistory({ subject, refreshKey = 0 }: { subject: AuditSubject; refreshKey?: number }) {
  const certiflow = useCertiflow();
  const history = useQuery(() => certiflow.audit.auditHistory(subject), [JSON.stringify(subject), refreshKey]);
  return (
    <section className="card">
      <h2>History</h2>
      <QueryView query={history}>{(entries) => <AuditEntriesTable entries={entries} />}</QueryView>
    </section>
  );
}
