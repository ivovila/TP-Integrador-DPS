import { useState } from 'react';
import { Link, useParams } from 'react-router';
import type { CriterionResponse } from '../../domain/inspection';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { CertificationEligibilityCard } from '../certification/CertificationEligibilityCard';
import { AuditHistory } from '../shared/AuditHistory';
import { StatusBadge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatDate, formatInstant, formatPerson } from '../shared/format';
import { Card, Details, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { ReadOnlyNotice } from '../shared/RoleNotices';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';
import { CriterionResponseCard } from './CriterionResponseCard';
import { describeScope } from './InspectionListPage';
import { RectificationForm } from './RectificationForm';

function groupBySection(responses: CriterionResponse[]): [string, CriterionResponse[]][] {
  const sectionTitles = [...new Set(responses.map((response) => response.sectionTitle))];
  return sectionTitles.map((title) => [title, responses.filter((response) => response.sectionTitle === title)]);
}

function RaisedFindings({ findingIds }: { findingIds: string[] | undefined }) {
  if (findingIds === undefined) {
    return null;
  }
  return (
    <Card title="Findings raised">
      {findingIds.length === 0 ? (
        <p className="muted">No non-conformities: no findings were raised.</p>
      ) : (
        <ul className="plain-list">
          {findingIds.map((findingId) => (
            <li key={findingId}>
              <Link to={`/findings/${findingId}`}>{findingId}</Link>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

export function InspectionDetailPage() {
  const certiflow = useCertiflow();
  const { actingPerson, permitted } = useSession();
  const inspectionId = useParams().id!;
  const closure = useCommand();
  const [changeCount, setChangeCount] = useState(0);
  const [raisedFindingIds, setRaisedFindingIds] = useState<string[] | undefined>(undefined);
  const inspection = useQuery(() => certiflow.inspections.findInspection(inspectionId), [inspectionId, changeCount]);
  const asset = useQuery(async () => {
    const found = await certiflow.inspections.findInspection(inspectionId);
    return certiflow.catalog.findAsset(found.assetCode);
  }, [inspectionId]);
  const vocabulary = useQuery(() => certiflow.schemas.vocabulary(), []);

  const recordChange = () => setChangeCount((count) => count + 1);

  function close(): void {
    let findingIds: string[] = [];
    closure.run(
      async () => {
        findingIds = await certiflow.inspections.closeInspection({ inspectionId, closedBy: actingPerson });
      },
      () => {
        setRaisedFindingIds(findingIds);
        recordChange();
      },
    );
  }

  return (
    <QueryView query={inspection}>
      {(foundInspection) => {
        const pendingCount = foundInspection.responses.filter((response) => response.outcome === 'PENDING').length;
        const assetType = asset.data?.type;
        const permittedActions = permitted(foundInspection.actions);
        return (
          <>
            <PageHeader
              title={
                <>
                  Inspection {foundInspection.id} <StatusBadge status={foundInspection.closed ? 'CLOSED' : 'OPEN'} />
                </>
              }
              subtitle={
                <>
                  Asset <Link to={`/assets/${encodeURIComponent(foundInspection.assetCode)}`}>{foundInspection.assetCode}</Link>
                  {asset.data !== undefined && ` · ${asset.data.name}`}
                </>
              }
            >
              {permittedActions.includes('generateInspectionAct') && (
                <Link className="button" to={`/inspections/${foundInspection.id}/act`}>
                  Inspection act
                </Link>
              )}
              {permittedActions.includes('closeInspection') && (
                <button className="primary" onClick={close} disabled={closure.running}>
                  Close inspection
                </button>
              )}
            </PageHeader>
            <ErrorBanner error={closure.error} />
            <ReadOnlyNotice offeredActions={foundInspection.actions} record="inspection" />
            <Card>
              <Details
                entries={[
                  ['Inspector', formatPerson(foundInspection.assignment.inspector)],
                  ['Planned date', formatDate(foundInspection.assignment.plannedDate)],
                  [
                    'Schema version',
                    assetType === undefined ? (
                      `v${foundInspection.schemaVersion}`
                    ) : (
                      <Link to={`/schemas/${encodeURIComponent(assetType)}/versions/${foundInspection.schemaVersion}`}>
                        {assetType} · v{foundInspection.schemaVersion}
                      </Link>
                    ),
                  ],
                  ['Scope', describeScope(foundInspection.assignment.scope)],
                  ['Criteria', `${foundInspection.responses.length - pendingCount} of ${foundInspection.responses.length} evaluated`],
                ]}
              />
            </Card>
            <RaisedFindings findingIds={raisedFindingIds} />
            {groupBySection(foundInspection.responses).map(([sectionTitle, responses]) => (
              <Card title={sectionTitle} key={sectionTitle}>
                {responses.map((response) => (
                  <CriterionResponseCard
                    key={response.criterion.code}
                    inspectionId={foundInspection.id}
                    response={response}
                    actions={permittedActions}
                    evidenceKinds={vocabulary.data?.evidenceKinds ?? [{ name: '', label: '' }]}
                    onRecorded={recordChange}
                  />
                ))}
              </Card>
            ))}
            {foundInspection.revisions.length > 0 && (
              <Card title="Revisions">
                <table className="data">
                  <thead>
                    <tr>
                      <th>Revision</th>
                      <th>Reason</th>
                      <th>Signed by</th>
                      <th>When</th>
                      <th>Non-conformities</th>
                    </tr>
                  </thead>
                  <tbody>
                    {foundInspection.revisions.map((revision) => (
                      <tr key={revision.number}>
                        <td>{revision.number}</td>
                        <td>{revision.reason}</td>
                        <td>{revision.sealedBy.name}</td>
                        <td>{formatInstant(revision.sealedAt)}</td>
                        <td>
                          {revision.outcomes
                            .filter((outcome) => outcome.outcome === 'OBSERVED' || outcome.outcome === 'REJECTED')
                            .map((outcome) => `${outcome.criterionCode} ${outcome.outcome.toLowerCase()}`)
                            .join(', ') || 'None'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            )}
            {permittedActions.includes('rectifyInspection') && (
              <Card title="Rectify an answer">
                <p className="muted">A closed inspection only changes through a rectification, which adds a revision.</p>
                <RectificationForm
                  inspectionId={foundInspection.id}
                  responses={foundInspection.responses}
                  onRectified={(findingIds) => {
                    setRaisedFindingIds(findingIds);
                    recordChange();
                  }}
                />
              </Card>
            )}
            <CertificationEligibilityCard inspectionId={foundInspection.id} refreshKey={changeCount} />
            <AuditHistory subject={{ kind: 'inspection', id: foundInspection.id }} refreshKey={changeCount} />
          </>
        );
      }}
    </QueryView>
  );
}
