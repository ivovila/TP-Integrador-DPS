import { useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router';
import type { Certificate, CertificateBasis, Suspension } from '../../domain/certification';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { AuditHistory } from '../shared/AuditHistory';
import { StatusBadge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatDate, formatInstant, formatPerson, formatScope } from '../shared/format';
import { Card, Details, PageHeader, TextField } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { ReadOnlyNotice } from '../shared/RoleNotices';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';
import { DecisionDetails } from './DecisionDetails';
import { GlobalCertificateForm } from './GlobalCertificateForm';
import { SectionLink } from '../shared/SectionLink';

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

  function renew(event: FormEvent): void {
    event.preventDefault();
    let renewedNumber = '';
    renewal.run(
      async () => {
        renewedNumber = await certiflow.certification.renewCertificate({
          currentNumber: number,
          inspectionId,
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
        <button className="primary" disabled={renewal.running || inspectionId === ''}>
          Renew certificate
        </button>
      </div>
    </form>
  );
}

export function BasisDescription({ basis }: { basis: CertificateBasis }) {
  return basis.kind === 'inspection' ? (
    <>
      based on inspection <SectionLink to={`/inspections/${basis.inspectionId}`}>{basis.inspectionId}</SectionLink>
    </>
  ) : (
    <>
      derived from{' '}
      {basis.partialCertificateNumbers.map((partialNumber, position) => (
        <span key={partialNumber}>
          {position > 0 && ', '}
          <SectionLink to={`/certificates/${partialNumber}`}>{partialNumber}</SectionLink>
        </span>
      ))}
    </>
  );
}

function GlobalRenewalForm({ certificate }: { certificate: Certificate }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const asset = useQuery(() => certiflow.catalog.findAsset(certificate.assetCode), [certificate.assetCode]);

  return (
    <QueryView query={asset}>
      {(foundAsset) => (
        <GlobalCertificateForm
          assetCode={foundAsset.code}
          assetType={foundAsset.type}
          submitLabel="Renew global certificate"
          submit={async (schemaVersion, partialCertificateNumbers) => {
            const renewedNumber = await certiflow.certification.renewGlobalCertificate({
              currentNumber: certificate.number,
              schemaVersion,
              partialCertificateNumbers,
              renewedBy: actingPerson,
            });
            navigate(`/certificates/${renewedNumber}`);
          }}
        />
      )}
    </QueryView>
  );
}

export function CertificateDetailPage() {
  const certiflow = useCertiflow();
  const { permitted } = useSession();
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
                Certificate {foundCertificate.number} <StatusBadge status={foundCertificate.status} />{' '}
                <StatusBadge status={foundCertificate.modality} />
              </>
            }
            subtitle={
              <>
                Asset <SectionLink to={`/assets/${encodeURIComponent(foundCertificate.assetCode)}`}>{foundCertificate.assetCode}</SectionLink> ·{' '}
                {formatScope(foundCertificate.scope)} · <BasisDescription basis={foundCertificate.basis} />
              </>
            }
          >
            <SectionLink className="button" to={`/certificates/${foundCertificate.number}/document`}>
              Certificate document
            </SectionLink>
          </PageHeader>
          {foundCertificate.status === 'DEPENDENCY_INVALID' && (
            <p className="warning-banner">
              One of the partial certificates behind this global certificate is no longer active, so the global certificate is
              not valid. Renew it from active partial certificates.
            </p>
          )}
          <Card>
            <Details
              entries={[
                ['Scope', formatScope(foundCertificate.scope)],
                ['Schema version', `v${foundCertificate.schemaVersion}`],
                ['Valid from', formatDate(foundCertificate.validFrom)],
                ['Valid until', formatDate(foundCertificate.validUntil)],
                ['Issued by', formatPerson(foundCertificate.issuedBy)],
                ['Issued at', formatInstant(foundCertificate.issuedAt)],
              ]}
            />
          </Card>
          <Card title="Decision">
            <DecisionDetails decision={foundCertificate.decision} />
          </Card>
          <ReadOnlyNotice offeredActions={foundCertificate.actions} record="certificate" />
          {(foundCertificate.suspensions.length > 0 || permitted(foundCertificate.actions).includes('suspendCertificate')) && (
            <Card title="Suspension">
              <SuspensionList suspensions={foundCertificate.suspensions} />
              {permitted(foundCertificate.actions).includes('suspendCertificate') && (
                <SuspensionForm number={foundCertificate.number} onSuspended={() => setChangeCount((count) => count + 1)} />
              )}
            </Card>
          )}
          {permitted(foundCertificate.actions).includes('renewCertificate') && (
            <Card title="Renewal">
              <p className="muted">
                Renewing issues a new certificate for the same scope and retires this one. It needs an eligible inspection closed
                after the one this certificate is based on; the new validity comes from the jurisdiction policy.
              </p>
              <RenewalForm number={foundCertificate.number} assetCode={foundCertificate.assetCode} />
            </Card>
          )}
          {permitted(foundCertificate.actions).includes('renewGlobalCertificate') && (
            <Card title="Renewal">
              <p className="muted">
                A global certificate is renewed from active partial certificates, at least one of them backed by a later
                inspection.
              </p>
              <GlobalRenewalForm certificate={foundCertificate} />
            </Card>
          )}
          <AuditHistory subject={{ kind: 'certificate', number: foundCertificate.number }} refreshKey={changeCount} />
        </>
      )}
    </QueryView>
  );
}
