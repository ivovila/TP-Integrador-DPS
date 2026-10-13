import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router';
import type { Section } from '../../domain/schema';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { instantFromLocalInput } from '../shared/format';
import { Card, PageHeader, TextField } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';
import { parseSubsystemCodes, SectionsEditor } from './SectionsEditor';

export function PublishVersionPage() {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const assetType = useParams().assetType!;
  const publication = useCommand();
  const vocabulary = useQuery(() => certiflow.schemas.vocabulary(), []);
  const schema = useQuery(() => certiflow.schemas.findSchema(assetType), [assetType]);
  const [sections, setSections] = useState<Section[] | undefined>(undefined);
  const [requiredSubsystems, setRequiredSubsystems] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState('');

  useEffect(() => {
    if (schema.data !== undefined) {
      const latest = schema.data.versions.find((version) => version.number === schema.data!.latestPublishedVersion)!;
      setSections(latest.sections);
      setRequiredSubsystems(latest.requiredSubsystems.join(', '));
    }
  }, [schema.data]);

  function publish(event: FormEvent): void {
    event.preventDefault();
    publication.run(
      () =>
        certiflow.schemas.publishSchemaVersion({
          assetType,
          sections: sections ?? [],
          requiredSubsystems: parseSubsystemCodes(requiredSubsystems),
          ...(effectiveFrom === '' ? {} : { effectiveFrom: instantFromLocalInput(effectiveFrom) }),
          publishedBy: actingPerson,
        }),
      () => navigate(`/schemas/${encodeURIComponent(assetType)}`),
    );
  }

  return (
    <QueryView query={schema}>
      {(foundSchema) => (
        <>
          <PageHeader
            title={`Publish version ${foundSchema.latestPublishedVersion + 1}`}
            subtitle={`${foundSchema.name} · starts from version ${foundSchema.latestPublishedVersion}. Inspections already assigned keep their version.`}
          />
          <QueryView query={vocabulary}>
            {(knownVocabulary) =>
              sections !== undefined && (
                <form className="stacked" onSubmit={publish}>
                  <ErrorBanner error={publication.error} />
                  <Card>
                    <div className="form-grid">
                      <TextField label="Effective from" type="datetime-local" value={effectiveFrom} onChange={setEffectiveFrom} />
                      <TextField
                        label="Required subsystems"
                        value={requiredSubsystems}
                        onChange={setRequiredSubsystems}
                        placeholder="ELECTRICAL, PRESSURE"
                      />
                    </div>
                    <p className="muted">
                      Leave the date empty to put the version in force as soon as it is published. A scheduled version applies
                      to inspections assigned from that moment on, and must take effect after every version already published.
                    </p>
                  </Card>
                  <SectionsEditor
                    sections={sections}
                    vocabulary={knownVocabulary}
                    subsystemCodes={parseSubsystemCodes(requiredSubsystems)}
                    onChange={setSections}
                  />
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
