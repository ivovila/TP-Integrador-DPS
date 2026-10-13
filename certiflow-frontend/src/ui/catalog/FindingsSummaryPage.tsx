import { useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { StatusBadge } from '../shared/Badge';
import { Card, Details, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';
import { SectionLink } from '../shared/SectionLink';

export function FindingsSummaryPage() {
  const certiflow = useCertiflow();
  const code = useParams().code!;
  const summary = useQuery(() => certiflow.findings.generateFindingsSummary(code), [code]);

  return (
    <QueryView query={summary}>
      {(findingsSummary) => (
        <>
          <PageHeader title="Findings summary" subtitle={<SectionLink to={`/assets/${encodeURIComponent(code)}`}>Asset {code}</SectionLink>}>
            <button onClick={() => window.print()}>Print</button>
          </PageHeader>
          <Card>
            <Details
              entries={[
                ['Findings', findingsSummary.findings.length],
                ['Open', findingsSummary.openFindings],
              ]}
            />
            <p className="muted">Whether an open finding blocks certification depends on the jurisdiction policy of the asset.</p>
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
                    <th>Subsystem</th>
                    <th>Responsible</th>
                    <th>Status</th>
                    <th>Overdue actions</th>
                  </tr>
                </thead>
                <tbody>
                  {findingsSummary.findings.map((line) => (
                    <tr key={line.findingId}>
                      <td>
                        <SectionLink to={`/findings/${line.findingId}`}>{line.findingId}</SectionLink>
                      </td>
                      <td>
                        <span className="criterion-code">{line.criterionCode}</span> {line.description}
                      </td>
                      <td>{line.severity}</td>
                      <td>{line.subsystemCode ?? 'All'}</td>
                      <td>{line.responsible.name}</td>
                      <td>
                        <StatusBadge status={line.open ? 'OPEN' : 'CLOSED'} />
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
