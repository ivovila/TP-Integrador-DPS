import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import type { Section } from '../../domain/schema';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { Card, PageHeader, TextField } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';
import { SectionsEditor } from './SectionsEditor';

export function CreateSchemaPage() {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const creation = useCommand();
  const vocabulary = useQuery(() => certiflow.schemas.vocabulary(), []);
  const [name, setName] = useState('');
  const [assetType, setAssetType] = useState('');
  const [sections, setSections] = useState<Section[]>([]);

  function create(event: FormEvent): void {
    event.preventDefault();
    creation.run(
      () => certiflow.schemas.createInspectionSchema({ name, assetType, sections, createdBy: actingPerson }),
      () => navigate(`/schemas/${encodeURIComponent(assetType)}`),
    );
  }

  return (
    <>
      <PageHeader title="Create inspection schema" />
      <QueryView query={vocabulary}>
        {(knownVocabulary) => (
          <form className="stacked" onSubmit={create}>
            <ErrorBanner error={creation.error} />
            <Card>
              <div className="form-grid">
                <TextField label="Schema name" value={name} onChange={setName} />
                <TextField label="Asset type" value={assetType} onChange={setAssetType} placeholder="Fire extinguisher" />
              </div>
            </Card>
            <SectionsEditor sections={sections} vocabulary={knownVocabulary} onChange={setSections} />
            <div>
              <button className="primary" disabled={creation.running}>
                Create schema
              </button>
            </div>
          </form>
        )}
      </QueryView>
    </>
  );
}
