import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { Card, PageHeader, TextField } from '../shared/Page';
import { nobody, PersonPicker } from '../shared/PersonPicker';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';

export function AssignInspectionPage() {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const [searchParameters] = useSearchParams();
  const assignment = useCommand();
  const [assetCode, setAssetCode] = useState(searchParameters.get('asset') ?? '');
  const [inspector, setInspector] = useState(nobody);
  const [plannedDate, setPlannedDate] = useState('');
  const [coversWholeSchema, setCoversWholeSchema] = useState(true);
  const [scopedSectionTitles, setScopedSectionTitles] = useState<string[]>([]);

  const inspectableAssets = useQuery(
    async () => (await certiflow.catalog.listAssets({})).filter((asset) => asset.actions.includes('assignInspection')),
    [],
  );
  const sectionTitles = useQuery(async () => {
    const asset = (await certiflow.catalog.listAssets({})).find((candidate) => candidate.code === assetCode);
    if (asset === undefined || !asset.actions.includes('assignInspection')) {
      return [];
    }
    const schema = await certiflow.schemas.findSchema(asset.type);
    return schema.versions.find((version) => version.number === schema.currentVersion)!.sections.map((section) => section.title);
  }, [assetCode]);

  function toggleSection(title: string): void {
    setScopedSectionTitles(
      scopedSectionTitles.includes(title)
        ? scopedSectionTitles.filter((existing) => existing !== title)
        : [...scopedSectionTitles, title],
    );
  }

  function assign(event: FormEvent): void {
    event.preventDefault();
    let assignedInspectionId = '';
    assignment.run(
      async () => {
        assignedInspectionId = await certiflow.inspections.assignInspection({
          assetCode,
          inspector,
          plannedDate,
          scope: coversWholeSchema ? { kind: 'full' } : { kind: 'sections', titles: scopedSectionTitles },
          assignedBy: actingPerson,
        });
      },
      () => navigate(`/inspections/${assignedInspectionId}`),
    );
  }

  return (
    <>
      <PageHeader title="Assign inspection" subtitle="The inspection captures the schema version in force today" />
      <Card>
        <form className="stacked" onSubmit={assign}>
          <ErrorBanner error={assignment.error} />
          <QueryView query={inspectableAssets}>
            {(assets) => (
              <div className="form-grid">
                <div className="field">
                  <label>
                    Asset
                    <select value={assetCode} onChange={(event) => setAssetCode(event.target.value)}>
                      <option value="">Choose an asset…</option>
                      {assets.map((asset) => (
                        <option key={asset.code} value={asset.code}>
                          {asset.code} · {asset.name}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
                <PersonPicker label="Inspector" person={inspector} onChange={setInspector} />
                <TextField label="Planned date" type="date" value={plannedDate} onChange={setPlannedDate} />
              </div>
            )}
          </QueryView>
          <div>
            <h3>Scope</h3>
            <label className="checkbox">
              <input type="radio" checked={coversWholeSchema} onChange={() => setCoversWholeSchema(true)} />
              Whole schema
            </label>
            <label className="checkbox">
              <input type="radio" checked={!coversWholeSchema} onChange={() => setCoversWholeSchema(false)} />
              Only some sections (a partial inspection cannot back a certificate)
            </label>
            {!coversWholeSchema &&
              (sectionTitles.data ?? []).map((title) => (
                <label className="checkbox" key={title}>
                  <input type="checkbox" checked={scopedSectionTitles.includes(title)} onChange={() => toggleSection(title)} />
                  {title}
                </label>
              ))}
          </div>
          <div>
            <button className="primary" disabled={assignment.running}>
              Assign inspection
            </button>
          </div>
        </form>
      </Card>
    </>
  );
}
