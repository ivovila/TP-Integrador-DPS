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
import { parseSubsystemCodes, SectionsEditor } from './SectionsEditor';

export function CreateSchemaPage() {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const creation = useCommand();
  const vocabulary = useQuery(() => certiflow.schemas.vocabulary(), []);
  const [name, setName] = useState('');
  const [assetType, setAssetType] = useState('');
  const [sections, setSections] = useState<Section[]>([]);
  const [requiredSubsystems, setRequiredSubsystems] = useState('');

  function create(event: FormEvent): void {
    event.preventDefault();
    creation.run(
      () =>
        certiflow.schemas.createInspectionSchema({
          name,
          assetType,
          sections,
          requiredSubsystems: parseSubsystemCodes(requiredSubsystems),
          createdBy: actingPerson,
        }),
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
                <TextField
                  label="Required subsystems"
                  value={requiredSubsystems}
                  onChange={setRequiredSubsystems}
                  placeholder="ELECTRICAL, PRESSURE"
                />
              </div>
              <p className="muted">
                With subsystems, every criterion must belong to one of them (through its section or on its own) and the asset is
                certified per subsystem.
              </p>
            </Card>
            <SectionsEditor
              sections={sections}
              vocabulary={knownVocabulary}
              subsystemCodes={parseSubsystemCodes(requiredSubsystems)}
              onChange={setSections}
            />
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
