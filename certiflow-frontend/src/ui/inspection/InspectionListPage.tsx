import { useState } from 'react';
import { Link } from 'react-router';
import type { InspectionScope } from '../../domain/inspection';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { StatusBadge } from '../shared/Badge';
import { formatDate } from '../shared/format';
import { Card, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';

type StatusFilter = '' | 'open' | 'closed';

export function describeScope(scope: InspectionScope): string {
  return scope.kind === 'full' ? 'Whole schema' : scope.titles.join(', ');
}

export function InspectionListPage() {
  const certiflow = useCertiflow();
  const { mayDo } = useSession();
  const [assetCode, setAssetCode] = useState('');
  const [status, setStatus] = useState<StatusFilter>('');
  const assets = useQuery(() => certiflow.catalog.listAssets({}), []);
  const inspections = useQuery(
    () =>
      certiflow.inspections.listInspections({
        ...(assetCode === '' ? {} : { assetCode }),
        ...(status === '' ? {} : { status }),
      }),
    [assetCode, status],
  );

  return (
    <>
      <PageHeader title="Inspections">
        <div className="field">
          <label>
            Asset
            <select value={assetCode} onChange={(event) => setAssetCode(event.target.value)}>
              <option value="">All assets</option>
              {(assets.data ?? []).map((asset) => (
                <option key={asset.code} value={asset.code}>
                  {asset.code} · {asset.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="field">
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value as StatusFilter)}>
              <option value="">Any status</option>
              <option value="open">Open</option>
              <option value="closed">Closed</option>
            </select>
          </label>
        </div>
        {mayDo('assignInspection') && (
          <Link className="button primary" to="/inspections/new">
            Assign inspection
          </Link>
        )}
      </PageHeader>
      <Card>
        <QueryView query={inspections}>
          {(listedInspections) =>
            listedInspections.length === 0 ? (
              <EmptyState>No inspections match.</EmptyState>
            ) : (
              <table className="data">
                <thead>
                  <tr>
                    <th>Inspection</th>
                    <th>Asset</th>
                    <th>Planned</th>
                    <th>Inspector</th>
                    <th>Schema version</th>
                    <th>Scope</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {listedInspections.map((inspection) => (
                    <tr key={inspection.id}>
                      <td>
                        <Link to={`/inspections/${inspection.id}`}>{inspection.id}</Link>
                      </td>
                      <td>
                        <Link to={`/assets/${encodeURIComponent(inspection.assetCode)}`}>{inspection.assetCode}</Link>
                      </td>
                      <td>{formatDate(inspection.assignment.plannedDate)}</td>
                      <td>{inspection.assignment.inspector.name}</td>
                      <td>v{inspection.schemaVersion}</td>
                      <td>{describeScope(inspection.assignment.scope)}</td>
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
    </>
  );
}
