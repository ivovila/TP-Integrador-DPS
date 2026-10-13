import { Link, useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { AuditHistory } from '../shared/AuditHistory';
import { StatusBadge } from '../shared/Badge';
import { formatDate, formatPerson } from '../shared/format';
import { Card, Details, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';

export function AssetDetailPage() {
  const certiflow = useCertiflow();
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
            {foundAsset.actions.includes('assignInspection') && (
              <Link className="button primary" to={`/inspections/new?asset=${encodeURIComponent(code)}`}>
                Assign inspection
              </Link>
            )}
          </PageHeader>
          <Card>
            <Details
              entries={[
                ['Type', foundAsset.type],
                ['Location', foundAsset.location],
                ['Responsible', formatPerson(foundAsset.responsible)],
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
                        <th>Valid from</th>
                        <th>Valid until</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {assetCertificates.map((certificate) => (
                        <tr key={certificate.number}>
                          <td>
                            <Link to={`/certificates/${certificate.number}`}>{certificate.number}</Link>
                          </td>
                          <td>{formatDate(certificate.validFrom)}</td>
                          <td>{formatDate(certificate.validUntil)}</td>
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
          <AuditHistory subject={{ kind: 'asset', code }} />
        </>
      )}
    </QueryView>
  );
}
