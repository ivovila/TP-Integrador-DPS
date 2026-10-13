import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import type { Characteristic, Subsystem } from '../../domain/catalog';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { Card, PageHeader, TextField } from '../shared/Page';
import { nobody, PersonPicker } from '../shared/PersonPicker';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';

export function RegisterAssetPage() {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const registration = useCommand();
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [type, setType] = useState('');
  const [location, setLocation] = useState('');
  const [responsible, setResponsible] = useState(nobody);
  const [characteristics, setCharacteristics] = useState<Characteristic[]>([]);
  const [jurisdictionCode, setJurisdictionCode] = useState('');
  const [subsystems, setSubsystems] = useState<Subsystem[]>([]);
  const vocabulary = useQuery(() => certiflow.schemas.vocabulary(), []);

  function changeCharacteristic(position: number, change: Partial<Characteristic>): void {
    setCharacteristics(characteristics.map((existing, index) => (index === position ? { ...existing, ...change } : existing)));
  }

  function changeSubsystem(position: number, change: Partial<Subsystem>): void {
    setSubsystems(subsystems.map((existing, index) => (index === position ? { ...existing, ...change } : existing)));
  }

  function register(event: FormEvent): void {
    event.preventDefault();
    registration.run(
      () =>
        certiflow.catalog.registerAsset({
          code,
          name,
          type,
          location,
          responsible,
          characteristics,
          jurisdictionCode,
          subsystems,
          registeredBy: actingPerson,
        }),
      () => navigate(`/assets/${encodeURIComponent(code)}`),
    );
  }

  return (
    <>
      <PageHeader title="Register asset" />
      <Card>
        <form className="stacked" onSubmit={register}>
          <ErrorBanner error={registration.error} />
          <div className="form-grid">
            <TextField label="Code" value={code} onChange={setCode} placeholder="EXT-0001" />
            <TextField label="Name" value={name} onChange={setName} />
            <TextField label="Type" value={type} onChange={setType} placeholder="Fire extinguisher" />
            <TextField label="Location" value={location} onChange={setLocation} />
            <PersonPicker label="Responsible" person={responsible} onChange={setResponsible} role="assetResponsible" />
            <div className="field">
              <label>
                Jurisdiction
                <select value={jurisdictionCode} onChange={(event) => setJurisdictionCode(event.target.value)}>
                  <option value="">Choose a jurisdiction…</option>
                  {(vocabulary.data?.jurisdictions ?? []).map((jurisdiction) => (
                    <option key={jurisdiction.code} value={jurisdiction.code}>
                      {jurisdiction.code} · policy {jurisdiction.policyCode}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>
          <div>
            <h3>Subsystems</h3>
            <p className="muted">Each subsystem can be certified on its own. Leave empty for an asset that is certified as a whole.</p>
            {subsystems.map((subsystem, position) => (
              <div className="row" key={position}>
                <TextField
                  label="Subsystem code"
                  value={subsystem.code}
                  onChange={(value) => changeSubsystem(position, { code: value })}
                  placeholder="ELECTRICAL"
                />
                <TextField label="Subsystem name" value={subsystem.name} onChange={(value) => changeSubsystem(position, { name: value })} />
                <button type="button" className="quiet" onClick={() => setSubsystems(subsystems.filter((_, index) => index !== position))}>
                  Remove
                </button>
              </div>
            ))}
            <button type="button" onClick={() => setSubsystems([...subsystems, { code: '', name: '' }])}>
              Add subsystem
            </button>
          </div>
          <div>
            <h3>Characteristics</h3>
            {characteristics.map((characteristic, position) => (
              <div className="row" key={position}>
                <TextField label="Name" value={characteristic.name} onChange={(value) => changeCharacteristic(position, { name: value })} />
                <TextField label="Value" value={characteristic.value} onChange={(value) => changeCharacteristic(position, { value })} />
                <button
                  type="button"
                  className="quiet"
                  onClick={() => setCharacteristics(characteristics.filter((_, index) => index !== position))}
                >
                  Remove
                </button>
              </div>
            ))}
            <button type="button" onClick={() => setCharacteristics([...characteristics, { name: '', value: '' }])}>
              Add characteristic
            </button>
          </div>
          <div>
            <button className="primary" disabled={registration.running}>
              Register asset
            </button>
          </div>
        </form>
      </Card>
    </>
  );
}
