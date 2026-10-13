import { Link, useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { Badge, StatusBadge } from '../shared/Badge';
import { Card, Details, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';

export function FindingsSummaryPage() {
  const certiflow = useCertiflow();
  const code = useParams().code!;
  const summary = useQuery(() => certiflow.findings.generateFindingsSummary(code), [code]);

  return (
    <QueryView query={summary}>
      {(findingsSummary) => (
        <>
          <PageHeader title="Findings summary" subtitle={<Link to={`/assets/${encodeURIComponent(code)}`}>Asset {code}</Link>}>
            <button onClick={() => window.print()}>Print</button>
          </PageHeader>
          <Card>
            <Details
              entries={[
                ['Findings', findingsSummary.findings.length],
                ['Open', findingsSummary.openFindings],
                ['Blocking certification', findingsSummary.blockingFindings],
              ]}
            />
          </Card>
          <Card>
            {findingsSummary.findings.length === 0 ? (
              <EmptyState>This asset has no findings.</EmptyState>
            ) : (
              <table className="data">
                <thead>
                  <tr>
                    <th>Finding</th>
                    <th>Criterion</th>
                    <th>Severity</th>
                    <th>Responsible</th>
                    <th>Status</th>
                    <th>Overdue actions</th>
                  </tr>
                </thead>
                <tbody>
                  {findingsSummary.findings.map((line) => (
                    <tr key={line.findingId}>
                      <td>
                        <Link to={`/findings/${line.findingId}`}>{line.findingId}</Link>
                      </td>
                      <td>
                        <span className="criterion-code">{line.criterionCode}</span> {line.description}
                      </td>
                      <td>{line.severity}</td>
                      <td>{line.responsible.name}</td>
                      <td>
                        <StatusBadge status={line.open ? 'OPEN' : 'CLOSED'} />{' '}
                        {line.blocksCertification && <Badge tone="negative">blocks certification</Badge>}
                      </td>
                      <td>{line.overdueActions}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        </>
      )}
    </QueryView>
  );
}
