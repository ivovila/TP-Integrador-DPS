import type { Person } from '../../domain/shared';

export interface CredentialSource {
  headersFor(actingPerson: Person | undefined, path: string): Promise<Record<string, string>>;
  received(response: Response, actingPerson: Person | undefined, path: string): void;
  rejected(sentHeaders: Record<string, string>): void;
}

export const browserRequestHeader = { 'X-Requested-With': 'XMLHttpRequest' };

export function basicCredentials(personId: string, password: string): string {
  const bytes = new TextEncoder().encode(`${personId}:${password}`);
  return `Basic ${btoa(String.fromCharCode(...bytes))}`;
}

export function bearerTokenOf(response: Response): string | undefined {
  const authorization = response.headers.get('Authorization');
  return authorization?.startsWith('Bearer ') ? authorization.slice('Bearer '.length) : undefined;
}
