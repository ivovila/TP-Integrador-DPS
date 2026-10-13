import { Link, useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { formatInstant } from '../shared/format';
import { EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';
import { SectionsView } from './SectionsView';

export function SchemaVersionPage() {
  const certiflow = useCertiflow();
  const { assetType, number } = useParams();
  const schema = useQuery(() => certiflow.schemas.findSchema(assetType!), [assetType]);

  return (
    <QueryView query={schema}>
      {(foundSchema) => {
        const version = foundSchema.versions.find((candidate) => String(candidate.number) === number);
        return version === undefined ? (
          <EmptyState>
            {foundSchema.name} has no version {number}.
          </EmptyState>
        ) : (
          <>
            <PageHeader
              title={`${foundSchema.name} · version ${version.number}`}
              subtitle={
                <>
                  <Link to={`/schemas/${encodeURIComponent(foundSchema.assetType)}`}>{foundSchema.assetType}</Link> · published{' '}
                  {formatInstant(version.publishedAt)}
                </>
              }
            />
            <SectionsView sections={version.sections} />
          </>
        );
      }}
    </QueryView>
  );
}
