import { useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { AuditEntriesTable } from '../shared/AuditHistory';
import { StatusBadge } from '../shared/Badge';
import { formatDate, formatInstant, formatPerson } from '../shared/format';
import { Card, Details, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { formatAnswer } from '../shared/ruleKinds';
import { useQuery } from '../shared/useQuery';
import { describeScope } from './InspectionListPage';
import { SectionLink } from '../shared/SectionLink';

export function InspectionActPage() {
  const certiflow = useCertiflow();
  const inspectionId = useParams().id!;
  const act = useQuery(() => certiflow.inspections.generateInspectionAct(inspectionId), [inspectionId]);

  return (
    <>
      <PageHeader title="Inspection act" subtitle={<SectionLink to={`/inspections/${inspectionId}`}>Inspection {inspectionId}</SectionLink>}>
        <button onClick={() => window.print()}>Print</button>
      </PageHeader>
      <QueryView query={act}>
        {(inspectionAct) => (
          <>
            <Card>
              <Details
                entries={[
                  ['Asset', `${inspectionAct.assetCode} · ${inspectionAct.assetName}`],
                  ['Inspector', formatPerson(inspectionAct.assignment.inspector)],
                  ['Planned date', formatDate(inspectionAct.assignment.plannedDate)],
                  ['Scope', describeScope(inspectionAct.assignment.scope)],
                  ['Schema version', `v${inspectionAct.schemaVersion}`],
                  ['Revisions', inspectionAct.revisions.length],
                ]}
              />
            </Card>
            <Card title="Answers">
              <table className="data">
                <thead>
                  <tr>
                    <th>Criterion</th>
                    <th>Answer</th>
                    <th>Answered by</th>
                    <th>Evidence</th>
                    <th>Observations</th>
                    <th>Outcome</th>
                  </tr>
                </thead>
                <tbody>
                  {inspectionAct.lines.map((line) => (
                    <tr key={line.criterionCode}>
                      <td>
                        <span className="criterion-code">{line.criterionCode}</span> {line.criterionText}
                      </td>
                      <td>{formatAnswer(line.criterion.rule, line.answer)}</td>
                      <td>
                        {line.answeredBy.name}
                        <br />
                        <span className="muted">{formatInstant(line.answeredAt)}</span>
                      </td>
                      <td>{line.evidenceCount}</td>
                      <td>{line.observations.join('; ')}</td>
                      <td>
                        <StatusBadge status={line.outcome} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
            <Card title="Revisions">
              <table className="data">
                <thead>
                  <tr>
                    <th>Revision</th>
                    <th>Reason</th>
                    <th>Signed by</th>
                    <th>When</th>
                  </tr>
                </thead>
                <tbody>
                  {inspectionAct.revisions.map((revision) => (
                    <tr key={revision.number}>
                      <td>{revision.number}</td>
                      <td>{revision.reason}</td>
                      <td>{formatPerson(revision.sealedBy)}</td>
                      <td>{formatInstant(revision.sealedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
            <Card title="History">
              <AuditEntriesTable entries={inspectionAct.history} />
            </Card>
          </>
        )}
      </QueryView>
    </>
  );
}
