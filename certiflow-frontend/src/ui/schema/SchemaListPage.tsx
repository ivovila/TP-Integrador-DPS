import { Link } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { Card, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';

export function SchemaListPage() {
  const certiflow = useCertiflow();
  const { mayDo } = useSession();
  const schemas = useQuery(() => certiflow.schemas.listSchemas(), []);

  return (
    <>
      <PageHeader title="Inspection schemas" subtitle="One versioned template per asset type">
        {mayDo('createInspectionSchema') && (
          <Link className="button primary" to="/schemas/new">
            Create schema
          </Link>
        )}
      </PageHeader>
      <Card>
        <QueryView query={schemas}>
          {(listedSchemas) =>
            listedSchemas.length === 0 ? (
              <EmptyState>No schemas defined.</EmptyState>
            ) : (
              <table className="data">
                <thead>
                  <tr>
                    <th>Asset type</th>
                    <th>Schema</th>
                    <th>Current version</th>
                    <th>Versions</th>
                  </tr>
                </thead>
                <tbody>
                  {listedSchemas.map((schema) => (
                    <tr key={schema.assetType}>
                      <td>
                        <Link to={`/schemas/${encodeURIComponent(schema.assetType)}`}>{schema.assetType}</Link>
                      </td>
                      <td>{schema.name}</td>
                      <td>v{schema.currentVersion}</td>
                      <td>{schema.versions.length}</td>
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
