import { useCallback, useEffect, useState } from 'react';

export interface Query<Data> {
  data?: Data;
  error?: unknown;
  reload(): void;
}

export function useQuery<Data>(load: () => Promise<Data>, dependencies: unknown[]): Query<Data> {
  const [result, setResult] = useState<{ data?: Data; error?: unknown }>({});
  const [reloadCount, setReloadCount] = useState(0);

  useEffect(() => {
    let stillWanted = true;
    load().then(
      (data) => stillWanted && setResult({ data }),
      (error: unknown) => stillWanted && setResult({ error }),
    );
    return () => {
      stillWanted = false;
    };
  }, [...dependencies, reloadCount]);

  const reload = useCallback(() => setReloadCount((count) => count + 1), []);
  return { ...result, reload };
}
