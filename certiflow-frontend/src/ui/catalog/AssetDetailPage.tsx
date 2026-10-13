import { Link, useNavigate, useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { GlobalCertificateForm } from '../certification/GlobalCertificateForm';
import { AuditHistory } from '../shared/AuditHistory';
import { StatusBadge } from '../shared/Badge';
import { formatDate, formatPerson, formatScope } from '../shared/format';
import { Card, Details, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { ReadOnlyNotice } from '../shared/RoleNotices';
import { useQuery } from '../shared/useQuery';

export function AssetDetailPage() {
  const certiflow = useCertiflow();
  const { actingPerson, permitted } = useSession();
  const navigate = useNavigate();
  const code = useParams().code!;
  const asset = useQuery(() => certiflow.catalog.findAsset(code), [code]);
  const inspections = useQuery(() => certiflow.inspections.listInspections({ assetCode: code }), [code]);
  const certificates = useQuery(() => certiflow.certification.listCertificates({ assetCode: code }), [code]);

  return (
    <QueryView query={asset}>
      {(foundAsset) => (
        <>
          <PageHeader title={foundAsset.name} subtitle={`Asset ${foundAsset.code}`}>
            <Link className="button" to={`/assets/${encodeURIComponent(code)}/findings-summary`}>
              Findings summary
            </Link>
            {permitted(foundAsset.actions).includes('assignInspection') && (
              <Link className="button primary" to={`/inspections/new?asset=${encodeURIComponent(code)}`}>
                Assign inspection
              </Link>
            )}
          </PageHeader>
          <ReadOnlyNotice offeredActions={foundAsset.actions} record="asset" />
          <Card>
            <Details
              entries={[
                ['Type', foundAsset.type],
                ['Location', foundAsset.location],
                ['Responsible', formatPerson(foundAsset.responsible)],
                ['Jurisdiction', foundAsset.jurisdictionCode],
                [
                  'Subsystems',
                  foundAsset.subsystems.length === 0
                    ? 'None: certified as a whole'
                    : foundAsset.subsystems.map((subsystem) => `${subsystem.code} · ${subsystem.name}`).join(', '),
                ],
                ...foundAsset.characteristics.map(
                  (characteristic): [string, string] => [characteristic.name, characteristic.value],
                ),
              ]}
            />
            {!foundAsset.actions.includes('assignInspection') && (
              <p className="muted">This asset type has no inspection schema yet, so it cannot be inspected.</p>
            )}
          </Card>
          <Card title="Inspections">
            <QueryView query={inspections}>
              {(assetInspections) =>
                assetInspections.length === 0 ? (
                  <EmptyState>No inspections assigned.</EmptyState>
                ) : (
                  <table className="data">
                    <thead>
                      <tr>
                        <th>Inspection</th>
                        <th>Planned</th>
                        <th>Inspector</th>
                        <th>Schema version</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {assetInspections.map((inspection) => (
                        <tr key={inspection.id}>
                          <td>
                            <Link to={`/inspections/${inspection.id}`}>{inspection.id}</Link>
                          </td>
                          <td>{formatDate(inspection.assignment.plannedDate)}</td>
                          <td>{inspection.assignment.inspector.name}</td>
                          <td>v{inspection.schemaVersion}</td>
                          <td>
                            <StatusBadge status={inspection.closed ? 'CLOSED' : 'OPEN'} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )
              }
            </QueryView>
          </Card>
          <Card title="Certificates">
            <QueryView query={certificates}>
              {(assetCertificates) =>
                assetCertificates.length === 0 ? (
                  <EmptyState>No certificates issued.</EmptyState>
                ) : (
                  <table className="data">
                    <thead>
                      <tr>
                        <th>Number</th>
                        <th>Scope</th>
                        <th>Valid from</th>
                        <th>Valid until</th>
                        <th>Modality</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {assetCertificates.map((certificate) => (
                        <tr key={certificate.number}>
                          <td>
                            <Link to={`/certificates/${certificate.number}`}>{certificate.number}</Link>
                          </td>
                          <td>
                            {formatScope(certificate.scope)}
                            {certificate.basis.kind === 'partialCertificates' && <span className="muted"> · global</span>}
                          </td>
                          <td>{formatDate(certificate.validFrom)}</td>
                          <td>{formatDate(certificate.validUntil)}</td>
                          <td>
                            <StatusBadge status={certificate.modality} />
                          </td>
                          <td>
                            <StatusBadge status={certificate.status} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )
              }
            </QueryView>
          </Card>
          {permitted(foundAsset.actions).includes('issueGlobalCertificate') && (
            <Card title="Global certificate">
              <p className="muted">
                The global certificate is derived from one active partial certificate per required subsystem. It lasts until the
                earliest of them expires and stops being valid as soon as one of them does.
              </p>
              <GlobalCertificateForm
                assetCode={foundAsset.code}
                assetType={foundAsset.type}
                submitLabel="Issue global certificate"
                submit={async (schemaVersion, partialCertificateNumbers) => {
                  const number = await certiflow.certification.issueGlobalCertificate({
                    assetCode: foundAsset.code,
                    schemaVersion,
                    partialCertificateNumbers,
                    issuedBy: actingPerson,
                  });
                  navigate(`/certificates/${number}`);
                }}
              />
            </Card>
          )}
          <AuditHistory subject={{ kind: 'asset', code }} />
        </>
      )}
    </QueryView>
  );
}
