import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import type { Characteristic } from '../../domain/catalog';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { Card, PageHeader, TextField } from '../shared/Page';
import { nobody, PersonPicker } from '../shared/PersonPicker';
import { useCommand } from '../shared/useCommand';

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

  function changeCharacteristic(position: number, change: Partial<Characteristic>): void {
    setCharacteristics(characteristics.map((existing, index) => (index === position ? { ...existing, ...change } : existing)));
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
            <PersonPicker label="Responsible" person={responsible} onChange={setResponsible} />
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
