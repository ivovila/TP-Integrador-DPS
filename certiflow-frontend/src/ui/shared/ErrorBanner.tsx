import { DomainError } from '../../ports/DomainError';
import { RejectedRequestError } from '../../ports/RejectedRequestError';

export function ErrorBanner({ error }: { error: unknown }) {
  if (error === undefined) {
    return null;
  }
  if (error instanceof RejectedRequestError) {
    return (
      <div className="error-banner" role="alert">
        <strong>{error.message}</strong>
        <span className="error-name">{error.title}</span>
      </div>
    );
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
          {error.ineligibilities.map((ineligibility, position) => (
            <li key={position}>{ineligibility.detail}</li>
          ))}
        </ul>
      )}
      <span className="error-name">{error.name}</span>
    </div>
  );
}
