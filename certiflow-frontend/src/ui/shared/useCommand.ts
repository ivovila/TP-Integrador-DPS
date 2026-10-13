import { useState } from 'react';

export interface Command {
  error?: unknown;
  running: boolean;
  run(operation: () => Promise<unknown>, afterSuccess?: () => void): Promise<void>;
}

export function useCommand(): Command {
  const [error, setError] = useState<unknown>(undefined);
  const [running, setRunning] = useState(false);

  async function run(operation: () => Promise<unknown>, afterSuccess?: () => void): Promise<void> {
    setRunning(true);
    try {
      await operation();
      setError(undefined);
      afterSuccess?.();
    } catch (failure) {
      setError(failure);
    } finally {
      setRunning(false);
    }
  }

  return { error, running, run };
}
