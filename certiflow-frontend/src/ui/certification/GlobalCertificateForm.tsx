import { useState, type FormEvent } from 'react';
import { useCertiflow } from '../app/CertiflowContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatDate } from '../shared/format';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';

/**
 * Elige la versión del esquema y un parcial por cada subsistema que esa versión exige.
 * Solo ofrece parciales activos; el sistema vuelve a validar todo al emitir.
 */
export function GlobalCertificateForm({
  assetCode,
  assetType,
  submitLabel,
  submit,
}: {
  assetCode: string;
  assetType: string;
  submitLabel: string;
  submit(schemaVersion: number, partialCertificateNumbers: string[]): Promise<unknown>;
}) {
  const certiflow = useCertiflow();
  const command = useCommand();
  const schema = useQuery(() => certiflow.schemas.findSchema(assetType), [assetType]);
  const certificates = useQuery(() => certiflow.certification.listCertificates({ assetCode }), [assetCode]);
  const [chosenVersion, setChosenVersion] = useState<number | undefined>(undefined);
  const [chosenPartials, setChosenPartials] = useState<Record<string, string>>({});

  const versionsWithSubsystems = (schema.data?.versions ?? []).filter((version) => version.requiredSubsystems.length > 0);
  const schemaVersion = chosenVersion ?? versionsWithSubsystems.at(-1)?.number;
  const requiredSubsystems = versionsWithSubsystems.find((version) => version.number === schemaVersion)?.requiredSubsystems ?? [];

  function activePartialsOf(subsystemCode: string) {
    return (certificates.data ?? []).filter(
      (certificate) =>
        certificate.status === 'ACTIVE' &&
        certificate.schemaVersion === schemaVersion &&
        certificate.scope.kind === 'subsystem' &&
        certificate.scope.subsystemCode === subsystemCode,
    );
  }

  function send(event: FormEvent): void {
    event.preventDefault();
    const partialCertificateNumbers = requiredSubsystems.map((code) => chosenPartials[code] ?? activePartialsOf(code)[0]?.number ?? '');
    command.run(() => submit(schemaVersion!, partialCertificateNumbers.filter((number) => number !== '')));
  }

  if (schema.data === undefined || certificates.data === undefined) {
    return <ErrorBanner error={schema.error ?? certificates.error} />;
  }
  if (versionsWithSubsystems.length === 0) {
    return <p className="muted">No version of the {assetType} schema is split into subsystems.</p>;
  }

  return (
    <form className="stacked" onSubmit={send}>
      <ErrorBanner error={command.error} />
      <div className="row">
        <div className="field">
          <label>
            Schema version
            <select value={schemaVersion ?? ''} onChange={(event) => setChosenVersion(Number(event.target.value))}>
              {versionsWithSubsystems.map((version) => (
                <option key={version.number} value={version.number}>
                  Version {version.number}
                </option>
              ))}
            </select>
          </label>
        </div>
        {requiredSubsystems.map((subsystemCode) => (
          <div className="field" key={subsystemCode}>
            <label>
              Partial for {subsystemCode}
              <select
                value={chosenPartials[subsystemCode] ?? activePartialsOf(subsystemCode)[0]?.number ?? ''}
                onChange={(event) => setChosenPartials({ ...chosenPartials, [subsystemCode]: event.target.value })}
              >
                <option value="">None active</option>
                {activePartialsOf(subsystemCode).map((certificate) => (
                  <option key={certificate.number} value={certificate.number}>
                    {certificate.number} · {certificate.modality.toLowerCase()} until {formatDate(certificate.validUntil)}
                  </option>
                ))}
              </select>
            </label>
          </div>
        ))}
        <button className="primary" disabled={command.running || schemaVersion === undefined}>
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
