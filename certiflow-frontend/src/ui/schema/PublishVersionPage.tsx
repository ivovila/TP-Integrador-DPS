import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router';
import type { Section } from '../../domain/schema';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';
import { SectionsEditor } from './SectionsEditor';

export function PublishVersionPage() {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const assetType = useParams().assetType!;
  const publication = useCommand();
  const vocabulary = useQuery(() => certiflow.schemas.vocabulary(), []);
  const schema = useQuery(() => certiflow.schemas.findSchema(assetType), [assetType]);
  const [sections, setSections] = useState<Section[] | undefined>(undefined);

  useEffect(() => {
    if (schema.data !== undefined) {
      setSections(schema.data.versions.find((version) => version.number === schema.data!.currentVersion)!.sections);
    }
  }, [schema.data]);

  function publish(event: FormEvent): void {
    event.preventDefault();
    publication.run(
      () => certiflow.schemas.publishSchemaVersion({ assetType, sections: sections ?? [], publishedBy: actingPerson }),
      () => navigate(`/schemas/${encodeURIComponent(assetType)}`),
    );
  }

  return (
    <QueryView query={schema}>
      {(foundSchema) => (
        <>
          <PageHeader
            title={`Publish version ${foundSchema.currentVersion + 1}`}
            subtitle={`${foundSchema.name} · starts from version ${foundSchema.currentVersion}. Inspections already assigned keep their version.`}
          />
          <QueryView query={vocabulary}>
            {(knownVocabulary) =>
              sections !== undefined && (
                <form className="stacked" onSubmit={publish}>
                  <ErrorBanner error={publication.error} />
                  <SectionsEditor sections={sections} vocabulary={knownVocabulary} onChange={setSections} />
                  <div>
                    <button className="primary" disabled={publication.running}>
                      Publish version
                    </button>
                  </div>
                </form>
              )
            }
          </QueryView>
        </>
      )}
    </QueryView>
  );
}
