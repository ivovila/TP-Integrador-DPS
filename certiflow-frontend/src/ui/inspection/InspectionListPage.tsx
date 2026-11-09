import { useState } from 'react';
import type { InspectionScope, InspectionSummary } from '../../domain/inspection';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { StatusBadge } from '../shared/Badge';
import { formatDate, shortId } from '../shared/format';
import { Card, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';
import { SectionLink } from '../shared/SectionLink';

type StatusFilter = '' | 'open' | 'closed';

export function describeScope(scope: InspectionScope): string {
  return scope.kind === 'full' ? 'Whole schema' : scope.titles.join(', ');
}

/** Primero lo que hay que trabajar: las abiertas, por fecha prevista; después las cerradas, de la más reciente. */
function inWorkOrder(inspections: InspectionSummary[]): InspectionSummary[] {
  const open = inspections.filter((inspection) => !inspection.closed);
  const closed = inspections.filter((inspection) => inspection.closed);
  const byPlannedDate = (first: InspectionSummary, second: InspectionSummary) =>
    first.assignment.plannedDate.localeCompare(second.assignment.plannedDate);
  return [...open.sort(byPlannedDate), ...closed.sort(byPlannedDate).reverse()];
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
          <SectionLink className="button primary" to="/inspections/new">
            Assign inspection
          </SectionLink>
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
                    <th>Status</th>
                    <th>Asset</th>
                    <th>Planned</th>
                    <th>Inspector</th>
                    <th>Scope</th>
                    <th>Schema</th>
                    <th>Inspection</th>
                  </tr>
                </thead>
                <tbody>
                  {inWorkOrder(listedInspections).map((inspection) => (
                    <tr key={inspection.id} className={inspection.closed ? '' : 'actionable'}>
                      <td>
                        <StatusBadge status={inspection.closed ? 'CLOSED' : 'OPEN'} />
                      </td>
                      <td>
                        <SectionLink to={`/assets/${encodeURIComponent(inspection.assetCode)}`}>{inspection.assetCode}</SectionLink>
                      </td>
                      <td>{formatDate(inspection.assignment.plannedDate)}</td>
                      <td>{inspection.assignment.inspector.name}</td>
                      <td>{describeScope(inspection.assignment.scope)}</td>
                      <td>v{inspection.schemaVersion}</td>
                      <td>
                        <SectionLink to={`/inspections/${inspection.id}`} title={inspection.id}>
                          {inspection.closed ? 'View' : 'Open'} {shortId(inspection.id)}
                        </SectionLink>
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
