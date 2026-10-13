import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import type { Suspension } from '../../domain/certification';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { AuditHistory } from '../shared/AuditHistory';
import { StatusBadge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatDate, formatInstant, formatPerson } from '../shared/format';
import { Card, Details, PageHeader, TextField } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';

export function SuspensionList({ suspensions }: { suspensions: Suspension[] }) {
  return (
    <ul className="plain-list">
      {suspensions.map((suspension) => (
        <li key={suspension.suspendedAt}>
          {suspension.reason}{' '}
          <span className="muted">
            by {suspension.suspendedBy.name}, {formatInstant(suspension.suspendedAt)}
          </span>
        </li>
      ))}
    </ul>
  );
}

function SuspensionForm({ number, onSuspended }: { number: string; onSuspended(): void }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const suspension = useCommand();
  const [reason, setReason] = useState('');

  function suspend(event: FormEvent): void {
    event.preventDefault();
    suspension.run(
      () => certiflow.certification.suspendCertificate({ number, reason, suspendedBy: actingPerson }),
      () => {
        setReason('');
        onSuspended();
      },
    );
  }

  return (
    <form onSubmit={suspend}>
      <ErrorBanner error={suspension.error} />
      <div className="row">
        <TextField label="Reason for the suspension" value={reason} onChange={setReason} />
        <button disabled={suspension.running}>Suspend certificate</button>
      </div>
    </form>
  );
}

function RenewalForm({ number, assetCode }: { number: string; assetCode: string }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const renewal = useCommand();
  const closedInspections = useQuery(() => certiflow.inspections.listInspections({ assetCode, status: 'closed' }), [assetCode]);
  const [inspectionId, setInspectionId] = useState('');
  const [validUntil, setValidUntil] = useState('');

  function renew(event: FormEvent): void {
    event.preventDefault();
    let renewedNumber = '';
    renewal.run(
      async () => {
        renewedNumber = await certiflow.certification.renewCertificate({
          currentNumber: number,
          inspectionId,
          validUntil,
          renewedBy: actingPerson,
        });
      },
      () => navigate(`/certificates/${renewedNumber}`),
    );
  }

  return (
    <form onSubmit={renew}>
      <ErrorBanner error={renewal.error} />
      <div className="row">
        <div className="field">
          <label>
            Closed inspection backing the renewal
            <select value={inspectionId} onChange={(event) => setInspectionId(event.target.value)}>
              <option value="">Choose an inspection…</option>
              {(closedInspections.data ?? []).map((inspection) => (
                <option key={inspection.id} value={inspection.id}>
                  {inspection.id} · planned {formatDate(inspection.assignment.plannedDate)}
                </option>
              ))}
            </select>
          </label>
        </div>
        <TextField label="Valid until" type="date" value={validUntil} onChange={setValidUntil} />
        <button className="primary" disabled={renewal.running || inspectionId === ''}>
          Renew certificate
        </button>
      </div>
    </form>
  );
}

export function CertificateDetailPage() {
  const certiflow = useCertiflow();
  const number = useParams().number!;
  const [changeCount, setChangeCount] = useState(0);
  const certificate = useQuery(() => certiflow.certification.findCertificate(number), [number, changeCount]);

  return (
    <QueryView query={certificate}>
      {(foundCertificate) => (
        <>
          <PageHeader
            title={
              <>
                Certificate {foundCertificate.number} <StatusBadge status={foundCertificate.status} />
              </>
            }
            subtitle={
              <>
                Asset <Link to={`/assets/${encodeURIComponent(foundCertificate.assetCode)}`}>{foundCertificate.assetCode}</Link> ·
                based on inspection{' '}
                <Link to={`/inspections/${foundCertificate.inspectionId}`}>{foundCertificate.inspectionId}</Link>
              </>
            }
          >
            <Link className="button" to={`/certificates/${foundCertificate.number}/document`}>
              Certificate document
            </Link>
          </PageHeader>
          <Card>
            <Details
              entries={[
                ['Valid from', formatDate(foundCertificate.validFrom)],
                ['Valid until', formatDate(foundCertificate.validUntil)],
                ['Issued by', formatPerson(foundCertificate.issuedBy)],
                ['Issued at', formatInstant(foundCertificate.issuedAt)],
              ]}
            />
          </Card>
          {(foundCertificate.suspensions.length > 0 || foundCertificate.actions.includes('suspendCertificate')) && (
            <Card title="Suspension">
              <SuspensionList suspensions={foundCertificate.suspensions} />
              {foundCertificate.actions.includes('suspendCertificate') && (
                <SuspensionForm number={foundCertificate.number} onSuspended={() => setChangeCount((count) => count + 1)} />
              )}
            </Card>
          )}
          {foundCertificate.actions.includes('renewCertificate') && (
            <Card title="Renewal">
              <p className="muted">
                Renewing issues a new certificate and retires this one. It needs an eligible inspection closed after{' '}
                {foundCertificate.inspectionId}.
              </p>
              <RenewalForm number={foundCertificate.number} assetCode={foundCertificate.assetCode} />
            </Card>
          )}
          <AuditHistory subject={{ kind: 'certificate', number: foundCertificate.number }} refreshKey={changeCount} />
        </>
      )}
    </QueryView>
  );
}
