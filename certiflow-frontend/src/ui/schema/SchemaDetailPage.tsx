import { useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { AuditHistory } from '../shared/AuditHistory';
import { Badge } from '../shared/Badge';
import { formatInstant } from '../shared/format';
import { Card, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { ReadOnlyNotice } from '../shared/RoleNotices';
import { useQuery } from '../shared/useQuery';
import { SectionLink } from '../shared/SectionLink';

export function SchemaDetailPage() {
  const certiflow = useCertiflow();
  const { permitted } = useSession();
  const assetType = useParams().assetType!;
  const schema = useQuery(() => certiflow.schemas.findSchema(assetType), [assetType]);
  const schemaPath = `/schemas/${encodeURIComponent(assetType)}`;

  return (
    <QueryView query={schema}>
      {(foundSchema) => (
        <>
          <PageHeader title={foundSchema.name} subtitle={`Inspection schema for ${foundSchema.assetType}`}>
            {permitted(foundSchema.actions).includes('publishSchemaVersion') && (
              <SectionLink className="button primary" to={`${schemaPath}/versions/new`}>
                Publish new version
              </SectionLink>
            )}
          </PageHeader>
          <ReadOnlyNotice offeredActions={foundSchema.actions} record="schema" />
          <Card title="Versions">
            <table className="data">
              <thead>
                <tr>
                  <th>Version</th>
                  <th>Published</th>
                  <th>Effective from</th>
                  <th>Subsystems</th>
                  <th>Sections</th>
                  <th>Criteria</th>
                </tr>
              </thead>
              <tbody>
                {foundSchema.versions.map((version) => (
                  <tr key={version.number}>
                    <td>
                      <SectionLink to={`${schemaPath}/versions/${version.number}`}>Version {version.number}</SectionLink>{' '}
                      {version.number === foundSchema.currentVersion && <Badge tone="positive">in force</Badge>}
                      {version.number > foundSchema.currentVersion && <Badge tone="neutral">scheduled</Badge>}
                    </td>
                    <td>{formatInstant(version.publishedAt)}</td>
                    <td>{formatInstant(version.effectiveFrom)}</td>
                    <td>{version.requiredSubsystems.length === 0 ? 'None' : version.requiredSubsystems.join(', ')}</td>
                    <td>{version.sections.length}</td>
                    <td>{version.sections.flatMap((section) => section.criteria).length}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="muted">
              Each version is in force from its effective date until the next one takes effect. Inspections keep the version that
              was in force when they were assigned.
            </p>
          </Card>
          <AuditHistory subject={{ kind: 'schema', assetType }} />
        </>
      )}
    </QueryView>
  );
}
