import type { ReactNode } from 'react';

export type Tone = 'positive' | 'negative' | 'warning' | 'neutral';

const toneByStatusWord: Record<string, Tone> = {
  APPROVED: 'positive',
  ACTIVE: 'positive',
  ACCEPTED: 'positive',
  CLOSED: 'neutral',
  OPEN: 'warning',
  PENDING: 'neutral',
  OBSERVED: 'warning',
  SUSPENDED: 'warning',
  CANCELLED: 'neutral',
  RENEWED: 'neutral',
  REJECTED: 'negative',
  EXPIRED: 'negative',
  CONDITIONAL: 'warning',
  DEPENDENCY_INVALID: 'negative',
  SCHEDULED: 'neutral',
};

export function Badge({ tone, children }: { tone: Tone; children: ReactNode }) {
  return <span className={`badge tone-${tone}`}>{children}</span>;
}

export function StatusBadge({ status }: { status: string }) {
  return <Badge tone={toneByStatusWord[status] ?? 'neutral'}>{status.toLowerCase().replaceAll('_', ' ')}</Badge>;
}
