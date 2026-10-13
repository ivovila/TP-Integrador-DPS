import { Link, useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { AuditHistory } from '../shared/AuditHistory';
import { Badge } from '../shared/Badge';
import { formatInstant } from '../shared/format';
import { Card, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';

export function SchemaDetailPage() {
  const certiflow = useCertiflow();
  const assetType = useParams().assetType!;
  const schema = useQuery(() => certiflow.schemas.findSchema(assetType), [assetType]);
  const schemaPath = `/schemas/${encodeURIComponent(assetType)}`;

  return (
    <QueryView query={schema}>
      {(foundSchema) => (
        <>
          <PageHeader title={foundSchema.name} subtitle={`Inspection schema for ${foundSchema.assetType}`}>
            {foundSchema.actions.includes('publishSchemaVersion') && (
              <Link className="button primary" to={`${schemaPath}/versions/new`}>
                Publish new version
              </Link>
            )}
          </PageHeader>
          <Card title="Versions">
            <table className="data">
              <thead>
                <tr>
                  <th>Version</th>
                  <th>Published</th>
                  <th>Sections</th>
                  <th>Criteria</th>
                </tr>
              </thead>
              <tbody>
                {foundSchema.versions.map((version) => (
                  <tr key={version.number}>
                    <td>
                      <Link to={`${schemaPath}/versions/${version.number}`}>Version {version.number}</Link>{' '}
                      {version.number === foundSchema.currentVersion && <Badge tone="positive">in force</Badge>}
                    </td>
                    <td>{formatInstant(version.publishedAt)}</td>
                    <td>{version.sections.length}</td>
                    <td>{version.sections.flatMap((section) => section.criteria).length}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="muted">Inspections keep the version that was in force when they were assigned.</p>
          </Card>
          <AuditHistory subject={{ kind: 'schema', assetType }} />
        </>
      )}
    </QueryView>
  );
}
