import { useState } from 'react';
import { Link } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { Badge, StatusBadge } from '../shared/Badge';
import { Card, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';

export function FindingListPage() {
  const certiflow = useCertiflow();
  const [assetCode, setAssetCode] = useState('');
  const assets = useQuery(() => certiflow.catalog.listAssets({}), []);
  const findings = useQuery(() => certiflow.findings.listFindings(assetCode === '' ? {} : { assetCode }), [assetCode]);

  return (
    <>
      <PageHeader title="Findings" subtitle="Non-conformities raised when an inspection is closed or rectified">
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
      </PageHeader>
      <Card>
        <QueryView query={findings}>
          {(listedFindings) =>
            listedFindings.length === 0 ? (
              <EmptyState>No findings.</EmptyState>
            ) : (
              <table className="data">
                <thead>
                  <tr>
                    <th>Finding</th>
                    <th>Asset</th>
                    <th>Criterion</th>
                    <th>Severity</th>
                    <th>Responsible</th>
                    <th>Corrective actions</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {listedFindings.map((finding) => (
                    <tr key={finding.id}>
                      <td>
                        <Link to={`/findings/${finding.id}`}>{finding.id}</Link>
                      </td>
                      <td>
                        <Link to={`/assets/${encodeURIComponent(finding.assetCode)}`}>{finding.assetCode}</Link>
                      </td>
                      <td>
                        <span className="criterion-code">{finding.criterion.code}</span> {finding.criterion.text}
                      </td>
                      <td>{finding.severity.label}</td>
                      <td>{finding.responsible.name}</td>
                      <td>
                        {finding.correctiveActions.length}{' '}
                        {finding.correctiveActions.some((action) => action.overdue) && <Badge tone="negative">overdue</Badge>}
                      </td>
                      <td>
                        <StatusBadge status={finding.open ? 'OPEN' : 'CLOSED'} />{' '}
                        {finding.blocksCertification && <Badge tone="negative">blocks certification</Badge>}
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
