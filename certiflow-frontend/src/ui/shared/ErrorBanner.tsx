import { DomainError } from '../../ports/DomainError';

export function ErrorBanner({ error }: { error: unknown }) {
  if (error === undefined) {
    return null;
  }
  if (!(error instanceof DomainError)) {
    return (
      <div className="error-banner" role="alert">
        Something went wrong. Try again.
      </div>
    );
  }
  return (
    <div className="error-banner" role="alert">
      <strong>{error.message}</strong>
      {error.ineligibilities.length > 0 && (
        <ul>
          {error.ineligibilities.map((ineligibility) => (
            <li key={ineligibility.reason}>{ineligibility.detail}</li>
          ))}
        </ul>
      )}
      <span className="error-name">{error.name}</span>
    </div>
  );
}
