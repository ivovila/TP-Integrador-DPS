import type { ReactNode } from 'react';
import { ErrorBanner } from './ErrorBanner';
import type { Query } from './useQuery';

export function QueryView<Data>({ query, children }: { query: Query<Data>; children: (data: Data) => ReactNode }) {
  if (query.error !== undefined) {
    return <ErrorBanner error={query.error} />;
  }
  if (query.data === undefined) {
    return <p className="muted">Loading…</p>;
  }
  return <>{children(query.data)}</>;
}
